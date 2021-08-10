package simulator.component;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.Callback;
import simulator.entity.Process;

import java.util.Map;

public class ProcessController {

    static private int ID;
    private ObservableList<Process> processes;
    private ObservableList<Map.Entry<Integer, Integer>> delays;

    @FXML
    private Label idLabel;
    @FXML
    private TableView detailTable;
    @FXML
    private TableColumn idColumn;
    @FXML
    private TableColumn delayColumn;

    public void initData(ObservableList<Process> processes,int index){
        this.processes =processes;
        this.ID=processes.get(index).getId();
        idLabel.setText(String.valueOf(ID));

        idColumn.setCellValueFactory((Callback<TableColumn.CellDataFeatures<Map.Entry<Integer, Integer>, String>, ObservableValue<String>>)
                p -> new SimpleStringProperty(String.valueOf(p.getValue().getKey())));
        delayColumn.setCellValueFactory((Callback<TableColumn.CellDataFeatures<Map.Entry<Integer, Integer>, String>, ObservableValue<String>>)
                p -> new SimpleStringProperty(String.valueOf(p.getValue().getValue())));

        delays = FXCollections.observableArrayList(processes.get(index).getProcessDelayMap().entrySet());
        detailTable.setItems(delays);
    }

    @FXML
    protected void initialize() {
        delayColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        delayColumn.setOnEditCommit(
                (EventHandler<TableColumn.CellEditEvent<Map.Entry<Integer, Integer>, String>>) t -> {
                    t.getTableView().getItems().get(
                            t.getTablePosition().getRow()).setValue(Integer.valueOf(t.getNewValue()));

                    int id = t.getTableView().getItems().get(t.getTablePosition().getRow()).getKey();
                    processes.get(id).setDelay(ProcessController.ID, Integer.valueOf(t.getNewValue()));
                }
        );
    }

    @FXML
    protected void confirm(){

    }

    @FXML
    protected void cancel(){

    }

}
