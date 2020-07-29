package com.bank;

import org.sqlite.SQLiteDataSource;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Random;
import java.util.Scanner;

class Card {
    private final Random rand = new Random();

    private final Connect conn;
    private String number;
    private String pin;
    private long balance;

    public Card (Connect conn) {
        this.conn = conn;
    }

    public void createCard () {
        this.number = setID();
        this.pin = setPIN();
        this.balance = 0L;

        this.addAccount();
    }

    public Card setCard (String number, String pin, Long balance) {
        this.number = number;
        this.pin = pin;
        this.balance = balance;
        return this;
    }

    private String setID() {
        StringBuilder numberBuilder = new StringBuilder("400000");
        while (numberBuilder.length() < 15) {
            numberBuilder.append(rand.nextInt(9));
        }
        int sum = sumDigits (numberBuilder.toString());
        int checkSum = 10 * (int) Math.ceil( (double) sum / 10) - sum;
        numberBuilder.append (checkSum);
        if (validateCardNumber (numberBuilder.toString()) ) {
            return numberBuilder.toString();
        }
        return setID();
    }

    private String setPIN() {
        StringBuilder PIN = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            PIN.append(rand.nextInt(10));
        }
        return PIN.toString();
    }

    public String getID() {
        return this.number;
    }

    public long getBalance() {
        return this.balance;
    }

    private int sumDigits (String id) {
        int itr = 1;
        int sum = 0;
        for (char c : id.toCharArray()) {
            int n = (int) c - 48;
            if (itr%2 == 1) {
                n *= 2;
                n %= 9;
            }
            itr ++;
            sum += n;
        }
        return sum;
    }

    public boolean validateCardNumber (String id) {
        int sum = sumDigits(id);

        return sum % 10 == 0;
    }


    public void addAccount () {
        if(conn.updateQuery("INSERT INTO card (id, number, pin)" +
                "values (" + rand.nextInt(2000) + ", "
                + number + ", " + pin + ")")) {
            System.out.println("Your card has been created\n" +
                    "Your card number:\n" + number);
            System.out.println("Your card PIN:\n" + pin);
        }

    }

    public void addIncome (Long money) {
        this.balance += money;
        if(conn.updateQuery("UPDATE card" +
                "SET balance = " + this.balance +
                "WHERE number = " + this.number))
            System.out.println("Income was added");
    }

    public void doTransfer (Long amount) {
        if (this.balance < amount) {
            System.out.println("Not enough money!");
        }

    }

    public String closeAccount () {
        return "DELETE FROM card WHERE number = " + this.number;
    }
}

class Session {
    private String number;
    private Card card;
    private boolean isLogged;

    public void setSession (Card card) {
        this.card = card;
        this.number = card.getID();
        this.isLogged = true;
    }

    public boolean isLoggedIn () {
        return this.card != null && this.number != null && this.isLogged;
    }

    public void logOut () {
        this.card = null;
        this.number = null;
        this.isLogged = false;
    }

    public Card getCard () {
        return this.card;
    }
}

class Connect {

    private final SQLiteDataSource dataSource;

    public Connect(String url) {

        this.dataSource = new SQLiteDataSource();
        dataSource.setUrl(url);

        try (Connection conn = dataSource.getConnection()){
            if (conn.isValid(5)) {
                System.out.println("Connection is valid");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void passQuery (String query) {
        try (Connection conn = dataSource.getConnection()) {
            Statement stmt = conn.createStatement();
            stmt.execute(query);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public boolean updateQuery (String query) {
        try (Connection conn = dataSource.getConnection()) {
            Statement stmt = conn.createStatement();
            stmt.executeUpdate(query);
            return true;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return false;
    }

    public boolean isCardAvailable(String number) {
        String query = "SELECT number FROM card WHERE number = '" + number + "'";
        try (Connection conn = dataSource.getConnection()) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            if (rs.getString("number") != null && !rs.getString("number").isEmpty())
                return true;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        return false;
    }

    public Session checkLogin(String number, String pin) {
        String query = "SELECT number, pin, balance FROM card WHERE number = '" + number + "'";
        try (Connection conn = dataSource.getConnection()) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);

            String num = rs.getString("number");
            String pw = rs.getString("pin");
            Long b = rs.getLong("balance");

            if (num != null && num.equals(number) && pw.equals(pin)) {
                Session sess = new Session();
                sess.setSession(new Card(this).setCard(num, pw, b));
                return sess;
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return null;
    }
}

public class Main {

    static Session session = new Session();

    static void menu (Connect conn) {
        Scanner in = new Scanner(System.in);
        int choice = -1;
        Card creditCard = new Card(conn);

        while (choice != 0) {
            if (session == null || !session.isLoggedIn()) {
                System.out.println("1. Create an account\n" + "2. Log into account\n" + "0. Exit");
                choice = in.nextInt();

                switch (choice) {
                    case 1: {
                        creditCard.createCard();
                    }
                    break;

                    case 2: {
                        System.out.println("Enter your card number:");
                        String accNumber = in.next();
                        if (accNumber.length() != 16 || !creditCard.validateCardNumber(accNumber) || !conn.isCardAvailable(accNumber)) {
                            System.out.println("Credit Card invalid");
                        } else {
                            System.out.println("Enter your PIN:");
                            String pin = in.next();

                            session = conn.checkLogin(accNumber, pin);

                            if (session != null && session.isLoggedIn()) {
                                System.out.println("You have successfully logged in!");
                            } else {
                                System.out.println("Wrong PIN!");
                            }
                        }
                    }
                    break;
                }
            } else {
                System.out.println("1. Balance\n" + "2. Add income\n" + "3. Do transfer\n" +
                        "4. Close account\n" + "5. Log out\n" + "0. Exit");
                choice = in.nextInt();

                switch (choice) {
                    case 1: {
                        System.out.println("Balance: " + session.getCard().getBalance());
                    }
                    break;

                    case 2: {
                        System.out.println("Enter income:");
                        Long x = in.nextLong();
                        creditCard.addIncome(x);
                    }
                    break;

                    case 3: {
                        System.out.println("Enter card number:");
                        String number = in.next();
                        if(!conn.isCardAvailable(number)) {
                            System.out.println("Such a card does not exist.");
                        } else if (creditCard.getID().equals(number)) {
                            System.out.println("You can't transfer money to the same account!");
                        } else if (creditCard.validateCardNumber(number)) {
                            System.out.println("Probably you made mistake in the card number. Please try again!");
                        } else {
                            System.out.println("Enter how much money you want to transfer:");
                            long amount = in.nextLong();
                            creditCard.doTransfer(amount);
                        }
                    }
                    break;

                    case 4: {
                        conn.updateQuery(creditCard.closeAccount());
                        session.logOut();
                    }
                    break;

                    case 5: {
                        session.logOut();
                        System.out.println("You have successfully logged out!");
                    }
                    break;
                }
            }
        }

        in.close();

        System.out.println("Bye");
        System.exit(0);
    }

    public static void main(String[] args) {
        Connect conn = null;
        String fileName = "test.s3db";

        if (args.length >= 2 && args[0].equals("-fileName")) {
            fileName = args[1];
        }

        File db = new File(fileName);
        if(!db.exists()) {
            try {
                db.createNewFile();
            } catch (IOException e){
                e.printStackTrace();
            }
        }
        conn = new Connect("jdbc:sqlite:" + fileName);


        String tableCreationQuery = "CREATE TABLE IF NOT EXISTS card (\n" +
                "id INTEGER,\n" +
                "number TEXT,\n" +
                "pin TEXT,\n" +
                "balance INTEGER DEFAULT 0\n" +
                ")";

        assert conn != null;
        conn.passQuery(tableCreationQuery);

        menu (conn);
    }
}
