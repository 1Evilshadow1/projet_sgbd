package tn.esprit;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.sql.*;
import java.time.LocalDate;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) {
        TabPane tabPane = new TabPane();
        tabPane.getTabs().addAll(
                creerTabCommandes(),
                creerTabLivraisons(),
                creerTabArticles()
        );

        VBox root = new VBox(20,
                new Label("Projet SGBD 2025-2026 – Gestion Livraisons de Commandes"),
                new Label("Mohamed Taalouch – 2ING INFO"),
                new Separator(),
                tabPane
        );
        root.setPadding(new Insets(20));
        root.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 14px;");

        Scene scene = new Scene(root, 1200, 800);
        stage.setTitle("Gestion des Livraisons de Commandes");
        stage.setScene(scene);
        stage.show();
    }

    // ========================================================
    // TAB COMMANDES
    // ========================================================
    private Tab creerTabCommandes() {
        Tab tab = new Tab("Gestion des Commandes");
        tab.setClosable(false);

        TextField tfClient = new TextField();
        tfClient.setPromptText("N° client");
        Button btnAjouter = new Button("Ajouter Commande");
        Label lblResultat = new Label();

        TextField tfNumCmd = new TextField();
        tfNumCmd.setPromptText("N° commande");
        ComboBox<String> cbEtat = new ComboBox<>();
        cbEtat.getItems().addAll("PR", "LI", "SO", "AN", "AL");
        Button btnChangerEtat = new Button("Changer état");

        TextField tfRecherche = new TextField();
        tfRecherche.setPromptText("N° commande OU N° client OU date (YYYY-MM-DD)");
        Button btnRechercher = new Button("Rechercher");
        TableView<CommandeFX> table = creerTableCommandes();

        table.setPlaceholder(new Label("Aucune commande trouvée"));

        btnAjouter.setOnAction(e -> {
            try (CallableStatement cs = DB.getConn().prepareCall("{call pkg_commandes.ajouter_commande(?, ?)}")) {
                cs.setInt(1, Integer.parseInt(tfClient.getText().trim()));
                cs.registerOutParameter(2, Types.INTEGER);
                cs.execute();
                int nouveauNum = cs.getInt(2);
                lblResultat.setText("Commande n° " + nouveauNum + " créée avec succès !");
                tfClient.clear();
            } catch (Exception ex) {
                lblResultat.setText("Erreur : " + ex.getMessage());
            }
        });

        btnChangerEtat.setOnAction(e -> {
            try (CallableStatement cs = DB.getConn().prepareCall("{call pkg_commandes.modifier_etat(?, ?)}")) {
                cs.setInt(1, Integer.parseInt(tfNumCmd.getText().trim()));
                cs.setString(2, cbEtat.getValue());
                cs.execute();
                lblResultat.setText("État de la commande modifié avec succès");
            } catch (Exception ex) {
                lblResultat.setText("Erreur : " + ex.getMessage());
            }
        });

        btnRechercher.setOnAction(e -> rechercherCommandes(tfRecherche.getText().trim(), table));

        VBox content = new VBox(15,
                new HBox(10, new Label("N° Client :"), tfClient, btnAjouter),
                new HBox(10, new Label("N° Cmd :"), tfNumCmd, new Label("Nouvel état :"), cbEtat, btnChangerEtat),
                lblResultat,
                new Separator(),
                new HBox(10, new Label("Rechercher :"), tfRecherche, btnRechercher),
                new ScrollPane(table)
        );
        content.setPadding(new Insets(15));
        tab.setContent(content);
        return tab;
    }

    // ========================================================
    // TAB LIVRAISONS
    // ========================================================
    private Tab creerTabLivraisons() {
        Tab tab = new Tab("Gestion des Livraisons");
        tab.setClosable(false);

        TextField tfCmd = new TextField();
        tfCmd.setPromptText("N° commande (doit être PR)");
        DatePicker dpDate = new DatePicker(LocalDate.now().plusDays(1));
        TextField tfLivreur = new TextField();
        tfLivreur.setPromptText("ID livreur");
        ComboBox<String> cbPaiement = new ComboBox<>();
        cbPaiement.getItems().addAll("avant_livraison", "apres_livraison");
        Button btnPlanifier = new Button("Planifier Livraison");
        Label lblLiv = new Label();

        TextField tfRechLiv = new TextField();
        tfRechLiv.setPromptText("N° commande / ID livreur / date (YYYY-MM-DD)");
        Button btnRechLiv = new Button("Rechercher");
        TableView<LivraisonFX> tableLiv = creerTableLivraisons();
        tableLiv.setPlaceholder(new Label("Aucune livraison trouvée"));

        btnPlanifier.setOnAction(e -> {
            try (CallableStatement cs = DB.getConn().prepareCall(
                    "{call pkg_livraisons.ajouter_livraison(?,?,?,?)}")) {
                cs.setInt(1, Integer.parseInt(tfCmd.getText().trim()));
                cs.setDate(2, Date.valueOf(dpDate.getValue()));
                cs.setInt(3, Integer.parseInt(tfLivreur.getText().trim()));
                cs.setString(4, cbPaiement.getValue());
                cs.execute();
                lblLiv.setText("Livraison planifiée avec succès !");
                tfCmd.clear(); tfLivreur.clear();
            } catch (Exception ex) {
                lblLiv.setText("Erreur : " + ex.getMessage());
            }
        });

        btnRechLiv.setOnAction(e -> rechercherLivraisons(tfRechLiv.getText().trim(), tableLiv));

        VBox content = new VBox(15,
                new HBox(10, new Label("Commande :"), tfCmd, new Label("Date :"), dpDate),
                new HBox(10, new Label("Livreur ID :"), tfLivreur, new Label("Paiement :"), cbPaiement, btnPlanifier),
                lblLiv,
                new Separator(),
                new HBox(10, new Label("Recherche :"), tfRechLiv, btnRechLiv),
                new ScrollPane(tableLiv)
        );
        content.setPadding(new Insets(15));
        tab.setContent(content);
        return tab;
    }

    // ========================================================
    // TAB ARTICLES
    // ========================================================
    private Tab creerTabArticles() {
        Tab tab = new Tab("Gestion des Articles");
        tab.setClosable(false);

        TextField tfRef = new TextField(); tfRef.setPromptText("Réf (ex: A999)");
        TextField tfDes = new TextField(); tfDes.setPromptText("Désignation");
        TextField tfPa = new TextField(); tfPa.setPromptText("Prix d'achat");
        TextField tfPv = new TextField(); tfPv.setPromptText("Prix de vente");
        TextField tfTva = new TextField(); tfTva.setText("1");
        TextField tfCat = new TextField(); tfCat.setPromptText("Catégorie");
        TextField tfStock = new TextField(); tfStock.setText("0");
        Button btnAjouterArt = new Button("Ajouter Article");
        Label lblArt = new Label();

        TextField tfRechArt = new TextField();
        tfRechArt.setPromptText("Code (A1) / Désignation / Catégorie");
        Button btnRechArt = new Button("Rechercher");
        TableView<ArticleFX> tableArt = creerTableArticles();
        tableArt.setPlaceholder(new Label("Aucun article trouvé"));

        btnAjouterArt.setOnAction(e -> {
            try (CallableStatement cs = DB.getConn().prepareCall(
                    "{call pkg_articles.ajouter_article(?,?,?,?,?,?,?)}")) {
                cs.setString(1, tfRef.getText().trim().toUpperCase());
                cs.setString(2, tfDes.getText().trim());
                cs.setDouble(3, Double.parseDouble(tfPa.getText().trim()));
                cs.setDouble(4, Double.parseDouble(tfPv.getText().trim()));
                cs.setInt(5, Integer.parseInt(tfTva.getText().trim()));
                cs.setString(6, tfCat.getText().trim().toUpperCase());
                cs.setInt(7, Integer.parseInt(tfStock.getText().trim()));
                cs.execute();
                lblArt.setText("Article ajouté avec succès !");
                tfRef.clear(); tfDes.clear(); tfPa.clear(); tfPv.clear(); tfCat.clear(); tfStock.clear();
            } catch (Exception ex) {
                lblArt.setText("Erreur : " + ex.getMessage());
            }
        });

        btnRechArt.setOnAction(e -> rechercherArticles(tfRechArt.getText().trim(), tableArt));

        GridPane form = new GridPane();
        form.setHgap(12); form.setVgap(12);
        form.addRow(0, new Label("Référence :"), tfRef, new Label("Désignation :"), tfDes);
        form.addRow(1, new Label("Prix Achat :"), tfPa, new Label("Prix Vente :"), tfPv);
        form.addRow(2, new Label("Code TVA :"), tfTva, new Label("Catégorie :"), tfCat);
        form.addRow(3, new Label("Stock :"), tfStock, btnAjouterArt);
        form.add(lblArt, 0, 4, 4, 1);

        VBox content = new VBox(15, form,
                new HBox(10, new Label("Recherche :"), tfRechArt, btnRechArt),
                new ScrollPane(tableArt)
        );
        content.setPadding(new Insets(15));
        tab.setContent(content);
        return tab;
    }

    // ========================================================
    // RECHERCHES CORRIGÉES
    // ========================================================
    private void rechercherCommandes(String critere, TableView<CommandeFX> table) {
        table.getItems().clear();
        if (critere.isEmpty()) return;

        String sql;
        if (critere.matches("\\d{1,6}")) {
            sql = """
                SELECT c.nocde, c.noclt, cl.nomclt||' '||NVL(cl.prenomclt,'') AS client,
                       TO_CHAR(c.datecde,'YYYY-MM-DD') AS datecde, c.etatcde
                FROM commandes c JOIN clients cl ON c.noclt = cl.noclt
                WHERE c.nocde = ? OR c.noclt = ?
                ORDER BY c.nocde DESC
                """;
        } else if (critere.matches("\\d{4}-\\d{2}-\\d{2}")) {
            sql = """
                SELECT c.nocde, c.noclt, cl.nomclt||' '||NVL(cl.prenomclt,'') AS client,
                       TO_CHAR(c.datecde,'YYYY-MM-DD') AS datecde, c.etatcde
                FROM commandes c JOIN clients cl ON c.noclt = cl.noclt
                WHERE TRUNC(c.datecde) = TO_DATE(?, 'YYYY-MM-DD')
                ORDER BY c.nocde
                """;
        } else {
            table.getItems().add(new CommandeFX("—", "—", "Critère invalide", "—", "—"));
            return;
        }

        try (PreparedStatement ps = DB.getConn().prepareStatement(sql)) {
            if (critere.matches("\\d+")) {
                ps.setString(1, critere);
                ps.setString(2, critere);
            } else {
                ps.setString(1, critere);
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                table.getItems().add(new CommandeFX(
                    rs.getString("nocde"),
                    rs.getString("noclt"),
                    rs.getString("client"),
                    rs.getString("datecde"),
                    rs.getString("etatcde")
                ));
            }
            if (table.getItems().isEmpty()) {
                table.getItems().add(new CommandeFX("—", "—", "Aucune commande trouvée", "—", "—"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void rechercherLivraisons(String critere, TableView<LivraisonFX> table) {
        table.getItems().clear();
        if (critere.isEmpty()) return;

        String sql;
        if (critere.matches("\\d+")) {
            if (critere.length() <= 4) {
                sql = """
                    SELECT lc.nocde, TO_CHAR(lc.dateliv,'YYYY-MM-DD') AS dateliv,
                           p.nompers||' '||p.prenompers AS livreur, lc.modepay, lc.etatliv
                    FROM LivraisonCom lc JOIN personnel p ON lc.livreur = p.idpers
                    WHERE lc.livreur = ?
                    ORDER BY lc.dateliv DESC
                    """;
            } else {
                sql = """
                    SELECT lc.nocde, TO_CHAR(lc.dateliv,'YYYY-MM-DD') AS dateliv,
                           p.nompers||' '||p.prenompers AS livreur, lc.modepay, lc.etatliv
                    FROM LivraisonCom lc JOIN personnel p ON lc.livreur = p.idpers
                    WHERE lc.nocde = ?
                    ORDER BY lc.dateliv
                    """;
            }
        } else if (critere.matches("\\d{4}-\\d{2}-\\d{2}")) {
            sql = """
                SELECT lc.nocde, TO_CHAR(lc.dateliv,'YYYY-MM-DD') AS dateliv,
                       p.nompers||' '||p.prenompers AS livreur, lc.modepay, lc.etatliv
                FROM LivraisonCom lc JOIN personnel p ON lc.livreur = p.idpers
                WHERE TRUNC(lc.dateliv) = TO_DATE(?, 'YYYY-MM-DD')
                ORDER BY lc.nocde
                """;
        } else {
            table.getItems().add(new LivraisonFX("—", "—", "Critère invalide", "—", "—"));
            return;
        }

        try (PreparedStatement ps = DB.getConn().prepareStatement(sql)) {
            ps.setString(1, critere);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                table.getItems().add(new LivraisonFX(
                    rs.getString("nocde"),
                    rs.getString("dateliv"),
                    rs.getString("livreur"),
                    rs.getString("modepay"),
                    rs.getString("etatliv")
                ));
            }
            if (table.getItems().isEmpty()) {
                table.getItems().add(new LivraisonFX("—", "—", "Aucune livraison", "—", "—"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void rechercherArticles(String critere, TableView<ArticleFX> table) {
        table.getItems().clear();
        if (critere.isEmpty()) return;

        String sql;
        String param = critere.trim();

        if (param.length() == 4 && param.toUpperCase().startsWith("A")) {
            sql = "SELECT refart, designation, prixv, qtestk, supp FROM articles WHERE refart = ? AND supp = 'F'";
            param = param.toUpperCase();
        } else if (param.length() <= 10) {
            sql = "SELECT refart, designation, prixv, qtestk, supp FROM articles WHERE UPPER(categorie) = ? AND supp = 'F'";
            param = param.toUpperCase();
        } else {
            sql = "SELECT refart, designation, prixv, qtestk, supp FROM articles WHERE UPPER(designation) LIKE ? AND supp = 'F'";
            param = "%" + param.toUpperCase() + "%";
        }

        try (PreparedStatement ps = DB.getConn().prepareStatement(sql)) {
            ps.setString(1, param);
            ResultSet rs = ps.executeQuery();
            boolean found = false;
            while (rs.next()) {
                found = true;
                table.getItems().add(new ArticleFX(
                    rs.getString("refart"),
                    rs.getString("designation"),
                    rs.getDouble("prixv"),
                    rs.getInt("qtestk"),
                    rs.getString("supp").equals("T") ? "Oui" : "Non"
                ));
            }
            if (!found) {
                table.getItems().add(new ArticleFX("—", "Aucun article trouvé", 0, 0, ""));
            }
        } catch (Exception e) {
            table.getItems().add(new ArticleFX("!", "Erreur : " + e.getMessage(), 0, 0, ""));
            e.printStackTrace();
        }
    }

    // ========================================================
    // TABLES ET COLONNES
    // ========================================================
    private TableView<CommandeFX> creerTableCommandes() {
        TableView<CommandeFX> t = new TableView<>();
        t.getColumns().addAll(
                col("N° Commande", "nocde"),
                col("N° Client", "noclt"),
                col("Client", "client"),
                col("Date", "date"),
                col("État", "etat")
        );
        t.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        return t;
    }

    private TableView<LivraisonFX> creerTableLivraisons() {
        TableView<LivraisonFX> t = new TableView<>();
        t.getColumns().addAll(
                col("N° Commande", "nocde"),
                col("Date Livraison", "date"),
                col("Livreur", "livreur"),
                col("Mode Paiement", "paiement"),
                col("État", "etat")
        );
        t.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        return t;
    }

    private TableView<ArticleFX> creerTableArticles() {
        TableView<ArticleFX> t = new TableView<>();
        t.getColumns().addAll(
                col("Référence", "ref"),
                col("Désignation", "des"),
                col("Prix Vente", "prix"),
                col("Stock", "stock"),
                col("Supprimé", "supprime")
        );
        t.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        return t;
    }

    private <T> TableColumn<T, String> col(String titre, String prop) {
        TableColumn<T, String> c = new TableColumn<>(titre);
        c.setCellValueFactory(new PropertyValueFactory<>(prop));
        c.setMinWidth(100);
        return c;
    }

    // ========================================================
    // CLASSES POUR LES TABLEAUX
    // ========================================================
    public static class CommandeFX {
        private final String nocde, noclt, client, date, etat;
        public CommandeFX(String nocde, String noclt, String client, String date, String etat) {
            this.nocde = nocde; this.noclt = noclt; this.client = client; this.date = date; this.etat = etat;
        }
        public String getNocde() { return nocde; }
        public String getNoclt() { return noclt; }
        public String getClient() { return client; }
        public String getDate() { return date; }
        public String getEtat() { return etat; }
    }

    public static class LivraisonFX {
        private final String nocde, date, livreur, paiement, etat;
        public LivraisonFX(String nocde, String date, String livreur, String paiement, String etat) {
            this.nocde = nocde; this.date = date; this.livreur = livreur; this.paiement = paiement; this.etat = etat;
        }
        public String getNocde() { return nocde; }
        public String getDate() { return date; }
        public String getLivreur() { return livreur; }
        public String getPaiement() { return paiement; }
        public String getEtat() { return etat; }
    }

    public static class ArticleFX {
        private final String ref, des, prix, stock, supprime;
        public ArticleFX(String ref, String des, double prix, int stock, String supprime) {
            this.ref = ref; this.des = des;
            this.prix = String.format("%.2f DT", prix);
            this.stock = String.valueOf(stock);
            this.supprime = supprime;
        }
        public String getRef() { return ref; }
        public String getDes() { return des; }
        public String getPrix() { return prix; }
        public String getStock() { return stock; }
        public String getSupprime() { return supprime; }
    }

    public static void main(String[] args) {
        launch();
    }
}