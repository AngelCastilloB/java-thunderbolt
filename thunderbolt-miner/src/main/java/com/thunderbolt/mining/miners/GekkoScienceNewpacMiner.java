/*
 * MIT License
 *
 * Copyright (c) 2020 Angel Castillo.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.thunderbolt.mining.miners;

/* IMPORTS *******************************************************************/

import com.thunderbolt.common.Convert;
import com.thunderbolt.common.NumberSerializer;
import com.thunderbolt.mining.Job;
import com.thunderbolt.mining.contracts.IJobFinishListener;
import com.thunderbolt.mining.contracts.IMiner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usb4java.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/* IMPLEMENTATION ************************************************************/

/**
 * GekkoScience NEWPAC (Dual BM1387) USB Stickminer.
 */
public class GekkoScienceNewpacMiner implements IMiner
{
    // Constants.
    private static final byte READ_ENDPOINT   = 0x01;
    private static final byte WRITE_ENDPOINT  = 0x02;
    private static final int  BUSY_WAIT_DELAY = 100; //ms
    private static final byte FTDI_TYPE_OUT   = 0x40;
    private static final int  BLOCK_TAIL_SIZE = 16;

    private static final byte FTDI_REQUEST_RESET   = 0x00;
    private static final byte FTDI_REQUEST_DATA    = 0x04;
    private static final byte FTDI_REQUEST_BAUD    = 0x03;
    private static final byte FTDI_REQUEST_FLOW    = 0x02;
    private static final byte FTDI_REQUEST_BITMODE = 0x0b;

    private static final short FTDI_VALUE_RESET       = 0x0000;
    private static final short FTDI_VALUE_DATA_BTS    = 0x0008;
    private static final short FTDI_VALUE_BAUD_BTS    = 0x001A;
    private static final short FTDI_VALUE_FLOW        = 0x0000;
    private static final short FTDI_VALUE_PURGE_RX    = 0x0001;
    private static final short FTDI_VALUE_PURGE_TX    = 0x0002;
    private static final short FTDI_VALUE_CB1_HI      = 0x20f2;
    private static final short FTDI_VALUE_CB1_LOW     = 0x20f0;
    private static final short FTDI_INDEX_BAUD_115200 = 0x0002;

    // Static fields.
    private static final Logger s_logger = LoggerFactory.getLogger(GekkoScienceNewpacMiner.class);

    // Instance fields.
    private volatile boolean               m_isRunning   = false;
    private boolean                        m_isActive    = false;
    private final BlockingQueue<Job>       m_jobQueue    = new LinkedBlockingQueue<>();
    private final List<IJobFinishListener> m_listeners   = new ArrayList<>();
    private Thread                         m_thread      = null;
    private DeviceHandle                   m_handle      = null;
    private final Map<Short, Job>          m_runningJobs = new HashMap<>();

    /**
     * Initializes a new instance of the GekkoScienceNewpacMiner class.
     */
    public GekkoScienceNewpacMiner()
    {
    }

    /**
     * Starts the miner.
     *
     * @return true if the miner was started successfully; otherwise; false.
     */
    @Override
    public boolean start()
    {
        int result = LibUsb.init(null);

        if (result != LibUsb.SUCCESS)
        {
            s_logger.error("Unable to initialize libusb: Error {}.", result);
            return false;
        }

        Device device = findDevice((short)0x0403, (short)0x6015);

        if (device == null)
        {
            s_logger.debug("Device GekkoScience NEWPAC (Dual BM1387) not found.");
            return false;
        }

        m_handle = new DeviceHandle();

        result = LibUsb.open(device, m_handle);

        if (result != LibUsb.SUCCESS)
        {
            s_logger.error("Unable to open usb device: Error {}.", result);
            return false;
        }

        // Check if kernel driver must be detached
        boolean detach = LibUsb.hasCapability(LibUsb.CAP_SUPPORTS_DETACH_KERNEL_DRIVER) &&
                (LibUsb.kernelDriverActive(m_handle, 0) > 0);

        // Detach the kernel driver
        if (detach)
        {
            result = LibUsb.detachKernelDriver(m_handle,  0);
            if (result != LibUsb.SUCCESS) throw new LibUsbException("Unable to detach kernel driver", result);
        }

        result = LibUsb.claimInterface(m_handle, 0);

        if (result != LibUsb.SUCCESS)
        {
            s_logger.error("Unable to claim interface: Error {}.", result);
            return false;
        }

        m_isRunning = true;
        m_thread = new Thread(this::run);
        m_thread.start();
        reset();

        s_logger.debug("ASIC Miner Waiting for jobs.");
        m_isActive = true;
        return true;
    }

    /**
     * Stops the miner.
     */
    @Override
    public void stop()
    {
        m_isRunning = false;

        try
        {
            m_thread.join();
        }
        catch (InterruptedException e)
        {
            // Calling thread interrupted.
        }

        LibUsb.releaseInterface(m_handle, 0);
        LibUsb.close(m_handle);
    }

    /**
     * Gets the number of active jobs.
     *
     * @return The number of active jobs.
     */
    public int getActiveJobs()
    {
        return m_runningJobs.size();
    }

    /**
     * Gets whether this miner is running or not.
     *
     * @return true if is running; otherwise; false.
     */
    @Override
    public boolean isRunning()
    {
        return m_isRunning;
    }

    /**
     * Cancels all current jobs
     */
    @Override
    public void cancelAllJobs()
    {
        if (!m_isRunning)
            return;

        m_jobQueue.clear();
        m_runningJobs.clear();
    }

    /**
     * Queue work on the miner.
     *
     * @param job The job to be work on.
     */
    @Override
    public void queueJob(Job job)
    {
        m_jobQueue.add(job);
    }

    /**
     * Adds an event listener to be notified when a Job is done.
     *
     * @param listener The event listener.
     */
    @Override
    public void addJobFinishListener(IJobFinishListener listener)
    {
        synchronized (m_listeners)
        {
            m_listeners.add(listener);
        }
    }

    /**
     * Removes an event listener.
     *
     * @param listener The event listener to be removed.
     */
    @Override
    public void removeJobFinishListener(IJobFinishListener listener)
    {
        synchronized (m_listeners)
        {
            m_listeners.remove(listener);
        }
    }

    /**
     * Runs the miner.
     */
    private void run()
    {
        try
        {
            while (m_isRunning)
            {
                if (m_jobQueue.size() > 0 && m_isActive)
                {
                    Job job = m_jobQueue.take();
                    s_logger.info("Starting Job {}", job.getId());

                    byte[] data = new byte[BLOCK_TAIL_SIZE];
                    byte[] midstate = job.getMidstate();

                    System.arraycopy(job.getData(), 0, data, 0, BLOCK_TAIL_SIZE);

                    sendWork(job.getId(),
                            Convert.reverse(Convert.reverseEndian(data)),
                            Convert.reverse(Convert.reverseEndian(midstate)));

                    job.start();
                    m_runningJobs.put(job.getId(), job);
                }

                ByteBuffer buffer = ByteBuffer.allocateDirect(64);
                IntBuffer transfered = IntBuffer.allocate(1);

                LibUsb.bulkTransfer(m_handle, (byte) (LibUsb.ENDPOINT_IN | READ_ENDPOINT), buffer, transfered, 200);
                int read = transfered.get(0);
                if (read > 0)
                {
                    byte[] data = new byte[read];
                    buffer.get(data);

                    if (read > 8 && m_isActive)
                    {
                        long  nonce = 0;
                        short jobId = 0;

                        ByteBuffer responseBuffer = ByteBuffer.wrap(data);
                        responseBuffer.position(2);
                        nonce = responseBuffer.getInt();

                        responseBuffer.position(7);
                        jobId = responseBuffer.get();

                        if (!m_runningJobs.containsKey(jobId))
                            continue;

                        Job job = m_runningJobs.get(jobId);
                        job.setNonce(nonce);
                        job.setSolved(true);
                        job.finish();

                        for (IJobFinishListener listener: m_listeners)
                            listener.onJobFinish(job);

                        m_runningJobs.remove(jobId);
                    }
                }

                Thread.sleep(BUSY_WAIT_DELAY);
            }
        }
        catch (InterruptedException e)
        {
            s_logger.error("Miner interrupted.", e);
            cancelAllJobs();
        }

        stop();
    }

    /**
     * Resets the miner.
     */
    public boolean reset()
    {
        if (!m_isRunning)
            return false;

        try
        {
            ByteBuffer buffer = BufferUtils.allocateByteBuffer(0);

            LibUsb.controlTransfer(m_handle, FTDI_TYPE_OUT, FTDI_REQUEST_RESET, FTDI_VALUE_RESET, (short)0, buffer, 0);
            LibUsb.controlTransfer(m_handle, FTDI_TYPE_OUT, FTDI_REQUEST_DATA, FTDI_VALUE_DATA_BTS, (short)0, buffer, 0);
            LibUsb.controlTransfer(m_handle, FTDI_TYPE_OUT, FTDI_REQUEST_BAUD, FTDI_VALUE_BAUD_BTS, (short)0, buffer, 0);
            LibUsb.controlTransfer(m_handle, FTDI_TYPE_OUT, FTDI_REQUEST_FLOW, FTDI_VALUE_FLOW, (short)0, buffer, 0);
            LibUsb.controlTransfer(m_handle, FTDI_TYPE_OUT, FTDI_REQUEST_RESET, FTDI_VALUE_PURGE_TX, (short)0, buffer, 0);
            LibUsb.controlTransfer(m_handle, FTDI_TYPE_OUT, FTDI_REQUEST_RESET, FTDI_VALUE_PURGE_RX, (short)0, buffer, 0);
            LibUsb.controlTransfer(m_handle, FTDI_TYPE_OUT, FTDI_REQUEST_BITMODE, FTDI_VALUE_CB1_HI, (short)0, buffer, 0);
            LibUsb.controlTransfer(m_handle, FTDI_TYPE_OUT, FTDI_REQUEST_BITMODE, FTDI_VALUE_CB1_LOW, (short)0, buffer, 0);

            Thread.sleep(30);

            LibUsb.controlTransfer(m_handle, FTDI_TYPE_OUT, FTDI_REQUEST_BITMODE, FTDI_VALUE_CB1_HI, (short)0, buffer, 0);

            Thread.sleep(30);

            // Set frequency.
            sendCommand(new byte[] { 0x58, 0x09, 0x00, 0x1c, 0x00, 0x20, 0x01, 0x00, 0x1b });

            LibUsb.controlTransfer(m_handle, FTDI_TYPE_OUT, FTDI_REQUEST_BAUD, FTDI_INDEX_BAUD_115200, (short)0, buffer, 0);

            Thread.sleep(30);

            LibUsb.controlTransfer(m_handle, FTDI_TYPE_OUT, FTDI_REQUEST_BITMODE, (short)0x20f3, (short)0, buffer, 0);
            LibUsb.controlTransfer(m_handle, FTDI_TYPE_OUT, FTDI_REQUEST_BITMODE, FTDI_VALUE_CB1_HI, (short)0, buffer, 0);

            // Miner Init.
            sendCommand(new byte[] { 0x54, 0x05, 0x00, 0x00, 0x19 });

            // Set frequency.
            sendCommand(new byte[] { 0x58, 0x09, 0x00, 0x0c, 0x00, 0x20, 0x02, 0x41, 0x09 });

            // Sending chain inactive
            sendCommand(new byte[] { 0x55, 0x05, 0x00, 0x10 });
            sendCommand(new byte[] { 0x41, 0x05, 0x00, 0x00, 0x15 });
            sendCommand(new byte[] { 0x41, 0x05, 0x08, 0x00, 0x19 });
            sendCommand(new byte[] { 0x58, 0x09, 0x00, 0x1c, 0x40, 0x20, (byte)0x81, (byte)0x80, 0x09 });

            // Ramp up
            byte[] blockTail = new byte[] { 0x00, 0x00, 0x00, 0x00, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
                    (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF };

            byte[] midstate = new byte[32]; // All zero

            for (short i = 0; i < 116; ++i)
                sendWork(i, blockTail, midstate);
        }
        catch (InterruptedException e)
        {
            s_logger.error("Thread interrupted", e);
            return false;
        }

        return true;
    }

    /**
     * Sends work to the miner.
     *
     * @param id The id of the work.
     * @param blockTail The block tail portion of the data.
     * @param midstate The midstate.
     *
     * @return true if the job could be send; otherwise; false.
     */
    private boolean sendWork(short id, byte[] blockTail, byte[] midstate)
    {
        if (!m_isRunning)
            return false;

        byte[] prefix = new byte[] { 0x21, 0x36, 0x00, 0x01 };

        prefix[2] = (byte)id;
        byte[] payload = new byte[prefix.length + blockTail.length + midstate.length + 2];

        System.arraycopy(prefix, 0, payload, 0, prefix.length);
        System.arraycopy(blockTail, 0, payload, prefix.length, blockTail.length);
        System.arraycopy(midstate, 0, payload, prefix.length + blockTail.length, midstate.length);

        byte[] checksum = crc16(payload, 0, payload.length - 2);
        System.arraycopy(checksum, 0, payload, payload.length - 2, checksum.length);

        IntBuffer transfered = IntBuffer.allocate(1);

        ByteBuffer buffer = ByteBuffer.allocateDirect(payload.length);
        buffer.put(payload);

        int result = LibUsb.bulkTransfer(m_handle, (byte) (LibUsb.ENDPOINT_OUT | WRITE_ENDPOINT), buffer, transfered, 100);

        return result == LibUsb.SUCCESS && transfered.get() == payload.length;
    }

    /**
     * Sends a command to the miner via bulk transfer.
     *
     * @param command The command to be send.
     *
     * @return true if the command was sent; otherwise; false.
     */
    private boolean sendCommand(byte[] command)
    {
        if (!m_isRunning)
            return false;

        ByteBuffer buffer = ByteBuffer.allocateDirect(command.length);
        buffer.put(command);
        IntBuffer transfered = IntBuffer.allocate(1);
        int result = LibUsb.bulkTransfer(m_handle, WRITE_ENDPOINT, buffer, transfered, 100);

        try
        {
            Thread.sleep(10);
        }
        catch (InterruptedException e)
        {
            s_logger.error("Thread interrupted.", e);
            return false;
        }

        return result == LibUsb.SUCCESS && transfered.get() == command.length;
    }

    /**
     * Computes a CRC value for the given payload.
     *
     * @param data   The data to calculate the CRC to.
     * @param offset The offset inside the data where to start.
     * @param length The length of the data.
     *
     * @return The CRC bytes.
     */
    public static byte[] crc16(byte[] data, int offset, int length)
    {
        if (data == null || offset < 0 || offset > data.length - 1 || offset + length > data.length)
        {
            return new byte[2];
        }

        int crc = 0xFFFF;
        for (int i = 0; i < length; ++i)
        {
            crc ^= data[offset + i] << 8;

            for (int j = 0; j < 8; ++j)
            {
                crc = (crc & 0x8000) > 0 ? (crc << 1) ^ 0x1021 : crc << 1;
            }
        }
        return NumberSerializer.serialize((short)(crc & 0xFFFF));
    }

    /**
     * Finds a USB device matching the VID and PID.
     *
     * @param vendorId  The vendor id of the device.
     * @param productId The product id of the device.
     *
     * @return The device instance if found; otherwise; false.
     */
    public Device findDevice(short vendorId, short productId)
    {
        DeviceList list = new DeviceList();

        int result = 0;

        LibUsb.getDeviceList(null, list);

        Device deviceFound = null;
        try
        {
            for (Device device: list)
            {
                DeviceDescriptor descriptor = new DeviceDescriptor();
                result = LibUsb.getDeviceDescriptor(device, descriptor);

                if (result != LibUsb.SUCCESS)
                {
                    s_logger.error("Unable to read device descriptor {}", result);
                    return null;
                }

                if (descriptor.idVendor() == vendorId && descriptor.idProduct() == productId)
                {
                    deviceFound = device;
                    break;
                }
            }
        }
        finally
        {
            LibUsb.freeDeviceList(list, false);
        }

        if (deviceFound != null)
        {
            LibUsb.refDevice(deviceFound);
        }

        return deviceFound;
    }
}
