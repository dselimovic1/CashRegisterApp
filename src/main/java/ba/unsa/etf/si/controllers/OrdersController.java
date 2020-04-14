package ba.unsa.etf.si.controllers;

import ba.unsa.etf.si.App;
import ba.unsa.etf.si.models.Order;
import ba.unsa.etf.si.models.OrderItem;
import ba.unsa.etf.si.models.Product;
import ba.unsa.etf.si.models.Receipt;
import ba.unsa.etf.si.utility.HttpUtils;
import ba.unsa.etf.si.utility.javafx.JavaFXUtils;
import ba.unsa.etf.si.utility.json.ProductUtils;
import ba.unsa.etf.si.utility.interfaces.ReceiptLoader;
import com.jfoenix.controls.JFXButton;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.Callback;
import org.controlsfx.control.GridCell;
import org.controlsfx.control.GridView;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;

public class OrdersController {

    @FXML
    private JFXButton addBtn;
    @FXML
    private GridView<Order> grid;

    private ObservableList<Product> products;
    private ObservableList<Order> orders;
    private final ReceiptLoader receiptLoader;

    public OrdersController(ReceiptLoader receiptLoader) {
        this.receiptLoader = receiptLoader;
    }

    @FXML
    public void initialize() {
        grid.setCellFactory(new OrderCellFactory());
        grid.setHorizontalCellSpacing(20);
        grid.setVerticalCellSpacing(20);
        grid.setCellHeight(280);
        grid.setCellWidth(350);
        addBtn.setOnAction(e -> addOrder());
        getProducts();
    }

    private void addOrder() {
        Platform.runLater(() -> grid.getItems().add(new Order(1L, PrimaryController.currentUser.getUsername(), LocalDateTime.now())));
    }

    private void removeOrder(Order order) {
        Platform.runLater(() -> {
            showInformation("Warning", "Are you sure you want to delete the order? Action can not be undone.", Alert.AlertType.WARNING, ButtonType.YES, ButtonType.NO)
                    .ifPresent(p -> {
                        if(p.getButtonData() == ButtonBar.ButtonData.YES) deleteOrder(order);
                    });
        });
    }

    private void deleteOrder(Order order) {
        HttpRequest DELETE = HttpUtils.DELETE(App.DOMAIN + "/api/orders/" + order.getServerID(), "Authorization", "Bearer " + PrimaryController.currentUser.getToken());
        HttpUtils.send(DELETE, HttpResponse.BodyHandlers.ofString(), response -> {
            Platform.runLater(() -> grid.getItems().remove(order));
        }, () -> System.out.println("ERROR IN DELETING ORDER!"));
    }

    private void editOrder(Order order) {
        Parent root = null;
        FXMLLoader loader = new FXMLLoader(App.class.getResource("fxml/orderEditor.fxml"));
        loader.setControllerFactory(c -> new OrderEditorController(order, products));
        try {
            root = loader.load();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Stage stage = new Stage();
        stage.setResizable(false);
        assert root != null;
        stage.setScene(new Scene(root));
        stage.getScene().getStylesheets().add(App.class.getResource("css/notification.css").toExternalForm());
        JavaFXUtils.centerStage(stage, 1000, 1000);
        stage.show();
    }

    private void createReceiptFromOrder(Order order) {
        receiptLoader.onReceiptLoaded(new Receipt(order));
    }

    private void getOrders() {
        HttpRequest GET = HttpUtils.GET(App.DOMAIN + "/api/orders", "Authorization", "Bearer " + PrimaryController.currentUser.getToken());
        HttpUtils.send(GET, HttpResponse.BodyHandlers.ofString(), response -> {
            new Thread(() -> {
                orders = getOrdersFromJSON(response);
                Platform.runLater(() -> grid.setItems(orders));
            }).start();
        }, () -> System.out.println("ERROR!"));
    }

    private ObservableList<Order> getOrdersFromJSON(String response) {
        ObservableList<Order> orders = FXCollections.observableArrayList();
        JSONArray array = new JSONArray(response);
        for(int i = 0; i < array.length(); ++i) orders.add(getOrderFromJSON(array.getJSONObject(i)));
        return orders;
    }

    private Order getOrderFromJSON(JSONObject json) {
        Order order = new Order(json.getLong("id"), PrimaryController.currentUser.getUsername(), LocalDateTime.now());
        order.setOrderItemList(getOrderItemsFromJSON(json.getJSONArray("receiptItems")));
        return order;
    }

    private ArrayList<OrderItem> getOrderItemsFromJSON(JSONArray array) {
        ArrayList<OrderItem> items = new ArrayList<>();
        for(int i = 0; i < array.length(); ++i) items.add(getOrderItemFromJSON(array.getJSONObject(i)));
        return items;
    }

    private OrderItem getOrderItemFromJSON(JSONObject json) {
        return new OrderItem(getProductByID(json.getLong("id")), json.getDouble("quantity"));
    }

    private Product getProductByID(Long id) {
        return products.stream().filter(p -> p.getServerID().equals(id)).findFirst().orElseGet(Product::new);
    }

    private Optional<ButtonType> showInformation(String title, String text, Alert.AlertType type, ButtonType... types) {
        Alert alert = new Alert(type, "", types);
        alert.setTitle(title);
        alert.setHeaderText(text);
        alert.getDialogPane().getStylesheets().add(App.class.getResource("css/alert.css").toExternalForm());
        alert.getDialogPane().getStyleClass().add("dialog-pane");
        return alert.showAndWait();
    }

    private void getProducts() {
        HttpRequest GET = HttpUtils.GET(App.DOMAIN + "/api/products", "Authorization", "Bearer " + PrimaryController.currentUser.getToken());
        HttpUtils.send(GET, HttpResponse.BodyHandlers.ofString(), response -> {
            try {
                products = ProductUtils.getObservableProductListFromJSON(response);
                getOrders();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }, () -> System.out.println("ERROR!"));
    }

    public class OrderCell extends GridCell<Order> {

        @FXML private Label orderID, bartenderName, date;
        @FXML private JFXButton payBtn, addToOrderBtn, deleteOrderBtn;

        public OrderCell() {
            loadFXML();
        }

        private void loadFXML() {
            FXMLLoader loader = new FXMLLoader(App.class.getResource("fxml/order.fxml"));
            loader.setController(this);
            loader.setRoot(this);
            try {
                loader.load();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        protected void updateItem(Order order, boolean empty) {
            super.updateItem(order, empty);
            if (empty) {
                setText(null);
                setContentDisplay(ContentDisplay.TEXT_ONLY);
            } else {
                orderID.setText(Long.toString(order.getServerID()));
                bartenderName.setText(Double.toString(order.getTotalAmount()));
                //date.setText(order.getCreationDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
                payBtn.setOnAction(e -> OrdersController.this.createReceiptFromOrder(order));
                addToOrderBtn.setOnAction(e -> OrdersController.this.editOrder(order));
                deleteOrderBtn.setOnAction(e -> OrdersController.this.removeOrder(order));
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            }
        }
    }

    public class OrderCellFactory implements Callback<GridView<Order>, GridCell<Order>> {

        @Override
        public GridCell<Order> call(GridView<Order> orderGridView) {
            return new OrderCell();
        }
    }
}
