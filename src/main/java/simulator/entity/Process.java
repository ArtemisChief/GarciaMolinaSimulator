package simulator.entity;

import javafx.application.Platform;
import simulator.component.SimulatorController;

import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.HashMap;
import java.util.Map;

/* status
   false - off
   true - on  */

public class Process implements Runnable {
    private int id;
    private int coordinatorId;
    private boolean status;
    private Map<Integer, Integer> processDelayMap;

    private Map<Integer, PipedInputStream> inputStreamMap;
    private Map<Integer, PipedOutputStream> outputStreamMap;

    private boolean isElectionOver;
    private boolean isOK;

    private boolean isRunning;

    public Process(int id) {
        this.id = id;
        coordinatorId = -1;
        status = true;
        processDelayMap = new HashMap<>();

        inputStreamMap = new HashMap<>();
        outputStreamMap = new HashMap<>();

        isElectionOver = true;
        isOK = false;

        isRunning = false;
    }

    @Override
    public void run() {
        try {
            while (isRunning) {
                if(!status) {
                    Thread.sleep(SimulatorController.WaitTime);
                    continue;
                }

                // This is Coordinator, just send Heartbeat
                if (coordinatorId == id) {
                    for (Map.Entry<Integer, PipedOutputStream> entry : outputStreamMap.entrySet()) {
                        PipedOutputStream outputStream = entry.getValue();
                        outputStream.write("Heartbeat".getBytes());
                    }
                    outputLog("Sending Heartbeat to All Processes");

                    Thread.sleep(SimulatorController.Heartbeat);
                }
                // This is not Coordinator, receive Heartbeat otherwise start Election
                else {
                    // Wait for Messages
                    Thread.sleep(SimulatorController.WaitTime);

                    // Check Messages from Coordinator
                    if (inputStreamMap.containsKey(coordinatorId)) {
                        PipedInputStream inputStream = inputStreamMap.get(coordinatorId);
                        if (inputStream.available() > 0) {

                            byte[] data = new byte[1024];
                            inputStream.read(data);
                            String str = byteToStr(data);

                            if (str.contains("Heartbeat"))
                                continue;
                        }
                    }

                    // No Heartbeat from Coordinator, Start Election
                    outputLog("No Heartbeat from Coordinator...");

                    isElectionOver = false;
                    // Send Message to Processes that ID larger than this to check if they are alive
                    for (Map.Entry<Integer, PipedOutputStream> entry : outputStreamMap.entrySet()) {
                        int id = entry.getKey();

                        if (id > this.id) {
                            PipedOutputStream outputStream = entry.getValue();
                            outputStream.write("Election".getBytes());
                            outputLog("Sending Election to Process " + id + " and waits for OK...");
                        }
                    }

                    // Wait for sending Election
                    Thread.sleep(100);

                    // Check Messages from Processes that ID smaller than this whether there is already an Election
                    for (Map.Entry<Integer, PipedInputStream> entry : inputStreamMap.entrySet()) {
                        int id = entry.getKey();

                        if (id < this.id) {
                            outputLog("Checking Election from Process " + id);

                            PipedInputStream inputStream = entry.getValue();
                            if (inputStream.available() <= 0)
                                continue;

                            byte[] data = new byte[1024];
                            inputStream.read(data);
                            String str = byteToStr(data);

                            if (str.equals("Election")) {
                                PipedOutputStream outputStream = outputStreamMap.get(id);
                                outputStream.write("OK".getBytes());
                                outputLog("Received Election from Process " + id + ", Replying OK");
                            }
                        }
                    }

                    // Wait for OK
                    Thread.sleep(SimulatorController.WaitTime);

                    // Check OK from Processes that ID larger than this to confirm they are alive
                    for (Map.Entry<Integer, PipedInputStream> entry : inputStreamMap.entrySet()) {
                        int id = entry.getKey();

                        if (id > this.id) {
                            PipedInputStream inputStream = entry.getValue();
                            if (inputStream.available() <= 0)
                                continue;

                            byte[] data = new byte[1024];
                            inputStream.read(data);
                            String str = byteToStr(data);

                            if (str.contains("OK")) {
                                isOK = true;
                                outputLog("Receiving OK from Process " + id + ", quit Election and waits for Coordinator...");
                                break;
                            }
                        }
                    }

                    // If OK received then wait for Coordinator, otherwise this becomes the coordinator and send Messages
                    if (isOK) {
                        while (!isElectionOver) {
                            // Wait for Coordinator
                            Thread.sleep(SimulatorController.WaitTime);

                            // Check Coordinator from Processes that ID larger than this to get new Coordinator
                            for (Map.Entry<Integer, PipedInputStream> entry : inputStreamMap.entrySet()) {
                                int id = entry.getKey();

                                if (id > this.id) {
                                    PipedInputStream inputStream = entry.getValue();
                                    if (inputStream.available() <= 0)
                                        continue;

                                    byte[] data = new byte[1024];
                                    inputStream.read(data);
                                    String str = byteToStr(data);

                                    if (str.contains("Heartbeat")) {
                                        isOK = false;
                                        isElectionOver = true;
                                        coordinatorId = id;
                                        outputLog("Confirmed Coordinator from Process " + id + ", start waiting for Heartbeat...");
                                        break;
                                    }
                                }
                            }
                        }
                    } else {
                        outputLog("No OK received, This Process becomes Coordinator and sending Heartbeat to all");
                        isOK = false;
                        isElectionOver = true;
                        coordinatorId = id;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void outputLog(String message) {
        Platform.runLater(() -> SimulatorController.Logs.add("Process " + id + ": " + message));
    }

    public int getId() {
        return id;
    }

    public void changeStatus() {
        status = !status;
    }

    public boolean getStatus() {
        return status;
    }

    public void setDelay(int id, int delay) {
        processDelayMap.put(id, delay);
    }

    public void deleteDelay(int id) {
        processDelayMap.remove(id);
    }

    public Map<Integer, Integer> getProcessDelayMap() {
        return processDelayMap;
    }

    public void addInputStreamMap(int id, PipedInputStream inputStream) {
        inputStreamMap.put(id, inputStream);
    }

    public void addOutputStreamMap(int id, PipedOutputStream outputStream) {
        outputStreamMap.put(id, outputStream);
    }

    public void setRunning(boolean running) {
        isRunning = running;
    }

    public String byteToStr(byte[] buffer) {
        int length = 0;
        for (int i = 0; i < buffer.length; ++i) {
            if (buffer[i] == 0) {
                length = i;
                break;
            }
        }
        return new String(buffer, 0, length);
    }
}