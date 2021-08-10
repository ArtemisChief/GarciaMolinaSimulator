package simulator.component;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import simulator.entity.Process;

import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.List;

public class SimulatorController {
    public static int WaitTime;
    public static int Heartbeat;
    public static ObservableList<String> Logs;

    private final int defaultDelay = 80;

    private int count;

    private ObservableList<Process> processes;

    private List<Thread> threadList;

    @FXML
    private Button addProcessBtn;
    @FXML
    private Button startBtn;
    @FXML
    private Button stopBtn;
    @FXML
    private Button clearBtn;
    @FXML
    private TextField waitTimeTextField;
    @FXML
    private TextField heartbeatTextField;
    @FXML
    private TableView processTable;
    @FXML
    private TableColumn processColumn;
    @FXML
    private TableColumn statusColumn;
    @FXML
    private TableColumn removeColumn;
    @FXML
    private TableColumn detailsColumn;
    @FXML
    private ListView listView;

    @FXML
    protected void initialize() {
        this.count = 0;

        processes = FXCollections.observableArrayList();
        Logs = FXCollections.observableArrayList("Logs output below:");

        threadList = new ArrayList<>();

        processColumn.setCellValueFactory(new PropertyValueFactory<Process, Integer>("id"));

        statusColumn.setCellFactory((col) -> {
                    TableCell<Process, String> cell = new TableCell<Process, String>() {
                        @Override
                        protected void updateItem(String item, boolean empty) {
                            super.updateItem(item, empty);
                            CheckBox checkBox = new CheckBox();

                            checkBox.setOnMouseClicked((col) -> {
                                processes.get(getIndex()).changeStatus();

                            });

                            if (empty) {
                                setText(null);
                                setGraphic(null);
                            } else {
                                checkBox.setSelected(processes.get(getIndex()).getStatus());
                                this.setGraphic(checkBox);
                            }
                        }
                    };
                    return cell;
                }
        );

        removeColumn.setCellFactory((col) -> {
                    TableCell<Process, String> cell = new TableCell<Process, String>() {
                        @Override
                        protected void updateItem(String item, boolean empty) {
                            super.updateItem(item, empty);
                            Button button = new Button("Remove");
                            button.setStyle("-fx-background-color: #ff4d4d;-fx-text-fill: #ffffff");

                            button.setOnMouseClicked((col) -> {
                                int id = processes.get(getIndex()).getId();
                                for (Process process : processes) {
                                    process.deleteDelay(id);
                                }
                                processes.remove(getIndex());
                            });

                            if (empty) {
                                setText(null);
                                setGraphic(null);
                            } else {
                                this.setGraphic(button);
                            }
                        }
                    };
                    return cell;
                }
        );

        detailsColumn.setCellFactory((col) -> {
                    TableCell<Process, String> cell = new TableCell<Process, String>() {
                        @Override
                        protected void updateItem(String item, boolean empty) {
                            super.updateItem(item, empty);
                            Button button = new Button("Show Detail");
                            button.setStyle("-fx-background-color: #00bcff;-fx-text-fill: #ffffff");

                            button.setOnMouseClicked((col) -> {
                                try {
                                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/processView.fxml"));
                                    Stage stage = new Stage();
                                    stage.initModality(Modality.APPLICATION_MODAL);
                                    stage.setTitle("Process Detail");
                                    stage.setScene(new Scene(loader.load()));
                                    stage.setResizable(false);

                                    ProcessController controller = loader.getController();
                                    controller.initData(processes, getIndex());

                                    stage.show();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                            });

                            if (empty) {
                                setText(null);
                                setGraphic(null);
                            } else {
                                this.setGraphic(button);
                            }
                        }
                    };
                    return cell;
                }
        );

        processTable.setItems(processes);
        listView.setItems(Logs);
    }

    @FXML
    protected void addProcess() {
        Process newProcess = new Process(count);

        for (Process process : processes) {
            process.setDelay(count, defaultDelay);
            newProcess.setDelay(process.getId(), defaultDelay);
        }

        ++count;

        processes.add(newProcess);
    }

    private String constructPipes() {
        for (int i = 0; i < processes.size(); ++i) {
            Process processA = processes.get(i);
            int idA = processA.getId();

            for (int j = 0; j < processes.size(); ++j) {
                if (i == j)
                    continue;

                Process processB = processes.get(j);
                int idB = processB.getId();

                PipedInputStream inputStreamA = new PipedInputStream();
                PipedInputStream inputStreamB = new PipedInputStream();

                processA.addInputStreamMap(idB, inputStreamA);
                processB.addInputStreamMap(idA, inputStreamB);

                PipedOutputStream outputStreamA = new PipedOutputStream();
                PipedOutputStream outputStreamB = new PipedOutputStream();

                try {
                    processA.addOutputStreamMap(idB, outputStreamA);
                    processB.addOutputStreamMap(idA, outputStreamB);

                    inputStreamA.connect(outputStreamB);
                    inputStreamB.connect(outputStreamA);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        int size = processes.size();

        return "Constructed and connected " + size * size + " pipes for input and output between " + size + " processes";
    }

    private String startThreads() {
        for (Process process : processes) {
            process.setRunning(true);
            Thread thread = new Thread(process);
            threadList.add(thread);
            thread.start();
        }

        return "Created " + processes.size() + " processes as threads and starting simulation";
    }

    private String stopThreads(){
        for (Process process : processes) {
            process.setRunning(false);
        }

        return "Simulation stopped";
    }

    @FXML
    protected void startSimulation() {
        Logs.clear();

        if (processes.size() <= 1) {
            Logs.add("Can't start a simulation less than 2 processes");
            return;
        }

        startBtn.setDisable(true);
        stopBtn.setDisable(false);
        addProcessBtn.setDisable(true);
        clearBtn.setDisable(true);
        heartbeatTextField.setDisable(true);
        waitTimeTextField.setDisable(true);

        WaitTime = Integer.valueOf(waitTimeTextField.getText());
        Heartbeat = Integer.valueOf(heartbeatTextField.getText());

        Logs.add(constructPipes());

        Logs.add(startThreads());
    }

    @FXML
    protected void stopSimulation() {
        startBtn.setDisable(false);
        stopBtn.setDisable(true);
        addProcessBtn.setDisable(false);
        clearBtn.setDisable(false);
        heartbeatTextField.setDisable(false);
        waitTimeTextField.setDisable(false);

        Logs.add(stopThreads());
    }

    @FXML
    protected void clear() {
        count=0;
        processes.clear();
        threadList.clear();

        Logs.clear();
    }
}