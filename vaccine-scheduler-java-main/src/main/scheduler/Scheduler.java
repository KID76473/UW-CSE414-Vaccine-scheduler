package scheduler;

import scheduler.db.ConnectionManager;
import scheduler.model.Caregiver;
import scheduler.model.Patient;
import scheduler.model.Vaccine;
import scheduler.util.Util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;

public class Scheduler {

    // objects to keep track of the currently logged-in user
    // Note: it is always true that at most one of currentCaregiver and currentPatient is not null
    //       since only one user can be logged-in at a time
    private static Caregiver currentCaregiver = null;
    private static Patient currentPatient = null;

    public static void main(String[] args) throws SQLException {
        // printing greetings text
        System.out.println();
        System.out.println("Welcome to the COVID-19 Vaccine Reservation Scheduling Application!");
        System.out.println("*** Please enter one of the following commands ***");
        System.out.println("> create_patient <username> <password>");  //TODO: implement create_patient (Part 1)
        System.out.println("> create_caregiver <username> <password>");
        System.out.println("> login_patient <username> <password>");  // TODO: implement login_patient (Part 1)
        System.out.println("> login_caregiver <username> <password>");
        System.out.println("> search_caregiver_schedule <date>");  // TODO: implement search_caregiver_schedule (Part 2)
        System.out.println("> reserve <date> <vaccine>");  // TODO: implement reserve (Part 2)
        System.out.println("> upload_availability <date>");
        System.out.println("> cancel <appointment_id>");  // TODO: implement cancel (extra credit)
        System.out.println("> add_doses <vaccine> <number>");
        System.out.println("> show_appointments");  // TODO: implement show_appointments (Part 2)
        System.out.println("> logout");  // TODO: implement logout (Part 2)
        System.out.println("> quit");
        System.out.println();

        // read input from user
        BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            System.out.print("> ");
            String response = "";
            try {
                response = r.readLine();
            } catch (IOException e) {
                System.out.println("Please try again!");
            }
            // split the user input by spaces
            String[] tokens = response.split(" ");
            // check if input exists
            if (tokens.length == 0) {
                System.out.println("Please try again!");
                continue;
            }
            // determine which operation to perform
            String operation = tokens[0];
            if (operation.equals("create_patient")) {
                createPatient(tokens);
            } else if (operation.equals("create_caregiver")) {
                createCaregiver(tokens);
            } else if (operation.equals("login_patient")) {
                loginPatient(tokens);
            } else if (operation.equals("login_caregiver")) {
                loginCaregiver(tokens);
            } else if (operation.equals("search_caregiver_schedule")) {
                searchCaregiverSchedule(tokens);
            } else if (operation.equals("reserve")) {
                reserve(tokens);
            } else if (operation.equals("upload_availability")) {
                uploadAvailability(tokens);
            } else if (operation.equals("cancel")) {
                cancel(tokens);
            } else if (operation.equals("add_doses")) {
                addDoses(tokens);
            } else if (operation.equals("show_appointments")) {
                showAppointments(tokens);
            } else if (operation.equals("logout")) {
                logout(tokens);
            } else if (operation.equals("quit")) {
                System.out.println("Bye!");
                return;
            } else {
                System.out.println("Invalid operation name!");
            }
        }
    }

    private static void createPatient(String[] tokens) { // username, password
        if (tokens.length != 3) {
            System.out.println("Failed to create user.");
            return;
        }
        if (checkPasswordIllegal(tokens[2])){
            String username = tokens[1];
            String password = tokens[2];
            if (usernameExistsPatient(username)) {
                System.out.println("Username taken, try again!");
                return;
            }
            byte[] salt = Util.generateSalt();
            byte[] hash = Util.generateHash(password, salt);
            try {
                currentPatient = new Patient.PatientBuilder(username, salt, hash).build();
                currentPatient.create();
                System.out.println("Created user " + username);
            } catch (SQLException e) {
                System.out.println("Failed to create user.");
                e.printStackTrace();
            }
        }
        else{
            System.out.println("Please make sure your password:" +
                    "has at least 8 characters"+
                    "contains at least one special character from “!”, “@”, “#”, “?”" +
                    "contains a mixture of letters and characters");
            return;
        }
    }

    private static void createCaregiver(String[] tokens) {
        // create_caregiver <username> <password>
        // check 1: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Failed to create user.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        // check 2: check if the username has been taken already
        if (usernameExistsCaregiver(username)) {
            System.out.println("Username taken, try again!");
            return;
        }
        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        // create the caregiver
        try {
            currentCaregiver = new Caregiver.CaregiverBuilder(username, salt, hash).build();
            // save to caregiver information to our database
            currentCaregiver.saveToDB();
            System.out.println("Created user " + username);
        } catch (SQLException e) {
            System.out.println("Failed to create user.");
            e.printStackTrace();
        }
    }

    private static boolean usernameExistsCaregiver(String username) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT * FROM Caregivers WHERE Username = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            // returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static boolean usernameExistsPatient(String username) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT * FROM Patient WHERE Username = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static void loginPatient(String[] tokens) {
        if (currentCaregiver != null || currentPatient != null) {
            System.out.println("User already logged in.");
            return;
        }
        if (tokens.length != 3) {
            System.out.println("Login failed.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        Patient patient = null;
        try {
            patient = new Patient.PatientGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Login failed.");
            e.printStackTrace();
        }
        if (patient == null) {
            System.out.println("Login failed.");
        } else {
            System.out.println("Logged in as: " + username);
            currentPatient = patient;
        }
    }

    private static void loginCaregiver(String[] tokens) {
        // login_caregiver <username> <password>
        // check 1: if someone's already logged-in, they need to log out first
        if (currentCaregiver != null || currentPatient != null) {
            System.out.println("User already logged in.");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Login failed.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        Caregiver caregiver = null;
        try {
            caregiver = new Caregiver.CaregiverGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Login failed.");
            e.printStackTrace();
        }
        // check if the login was successful
        if (caregiver == null) {
            System.out.println("Login failed.");
        } else {
            System.out.println("Logged in as: " + username);
            currentCaregiver = caregiver;
        }
    }

    private static void searchCaregiverSchedule(String[] tokens) throws SQLException {
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first!");
            return;
        }
        else if (tokens.length != 2) {
            System.out.println("Please try again!");
            return;
        }
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        String availableCaregivers = "SELECT C.Username FROM Caregivers C, Availabilities A WHERE A.Time = ? AND C.Username = A.Username GROUP BY C.Username ORDER BY C.Username ASC";
        String availableVaccine = "SELECT * FROM Vaccines";
        try {
            PreparedStatement statement1 = con.prepareStatement(availableCaregivers);
            statement1.setString(1, tokens[1]);
            ResultSet result1 = statement1.executeQuery();
            if (result1.next()) { // if there is caregiver available, show vaccines
                System.out.println("Available Caregivers: " + result1.getString(1));
                PreparedStatement statement2 = con.prepareStatement(availableVaccine);
                ResultSet result2 = statement2.executeQuery();
                result2.next();
                System.out.println("Available Vaccines: " + result2.getString(1));
                System.out.println("Doses: " + result2.getString(2));
            }
            else { // if there is no available caregiver, do not show vaccines
                System.out.println("No available caregiver!");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SQLException();
        } finally {
            cm.closeConnection();
        }
    }

    private static void reserve(String[] tokens) throws SQLException {
        if (currentCaregiver != null) {
            System.out.println("Please login as a patient!");
            return;
        }
        else if (currentPatient == null) {
            System.out.println("Please login first!");
            return;
        }
        else if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        String reserve1 = "SELECT A.Username FROM Availabilities AS A WHERE A.Time = ? ORDER BY A.Username ASC";
        String reserve2 = "SELECT * FROM Vaccines WHERE Vaccines.Name = ?";
        try {
            PreparedStatement statement1 = con.prepareStatement(reserve1);;
            statement1.setString(1, tokens[1]);
            ResultSet result1 = statement1.executeQuery();

            PreparedStatement statement2 = con.prepareStatement(reserve2);
            statement2.setString(1, tokens[2]);
            ResultSet result2 = statement2.executeQuery();

            Boolean hasCaregiver = result1.next();
            Boolean hasVaccine = result2.next();
            if (Integer. parseInt(result2.getString(2)) == 0) {
                hasVaccine = false;
            }
            if (hasCaregiver && hasVaccine) { // if there is caregiver available, show vaccines
                String getId = "SELECT A.Id FROM Appointment A";
                PreparedStatement getIdStatement = con.prepareStatement(getId);
                ResultSet resultOfId = getIdStatement.executeQuery();
                int id = -1;
                while (resultOfId.next()) {
                    id = Integer.parseInt(resultOfId.getString("id"));
                }
                if (id == -1) {
                    id = 0;
                }
                else {
                    id++;
                }

                // insert new appointment
                String firstLine = result1.getString(1);
                String appointment = "INSERT INTO Appointment VALUES (?, ?, ?, ?, ?)";
                PreparedStatement update = con.prepareStatement(appointment);
                update.setString(1, Integer.toString(id)); // id
                update.setString(2, currentPatient.getUsername()); // patient
                update.setString(3, firstLine); // caregiver
                update.setString(4, tokens[2]); // vaccine
                update.setString(5, tokens[1]); // time
                update.executeUpdate();
                // consume one vaccine
                Vaccine vaccine = new Vaccine.VaccineGetter(tokens[2]).get();
                vaccine.decreaseAvailableDoses(1);
                // delete availability
                String deleteAvailability = "DELETE FROM Availabilities WHERE Username = ? AND Time = ?";
                PreparedStatement statement3 = con.prepareStatement(deleteAvailability);
                statement3.setString(1, firstLine);
                statement3.setString(2, tokens[1]);
                statement3.executeUpdate();
                System.out.println("Appointment ID: " + id);
                System.out.println("Caregiver username: " + firstLine);
            }
            else if (!hasCaregiver){ // if there is no available caregiver, do not show vaccines
                System.out.println("No available caregiver!");
            }
            else {
                System.out.println("No available vaccine!");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SQLException();
        } finally {
            cm.closeConnection();
        }
    }

    private static void uploadAvailability(String[] tokens) {
        // upload_availability <date>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 2 to include all information (with the operation name)
        if (tokens.length != 2) {
            System.out.println("Please try again!");
            return;
        }
        String date = tokens[1];
        try {
            Date d = Date.valueOf(date);
            currentCaregiver.uploadAvailability(d);
            System.out.println("Availability uploaded!");
        } catch (IllegalArgumentException e) {
            System.out.println("Please enter a valid date!");
        } catch (SQLException e) {
            System.out.println("Error occurred when uploading availability");
            e.printStackTrace();
        }
    }

    private static void cancel(String[] tokens) throws SQLException {
        if (currentCaregiver != null) {
            System.out.println("Please login as a patient!");
            return;
        }
        else if (currentPatient == null) {
            System.out.println("Please login first!");
            return;
        }
        else if (tokens.length != 2) {
            System.out.println("Please try again!");
            return;
        }
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        String check = "SELECT * FROM Appointment AS A WHERE A.Id = ? AND A.Patient = ?";
        String cancel = "DELETE FROM Appointment WHERE Appointment.Id = ?";
        String insert = "INSERT INTO Availabilities VALUES (?, ?)";

        try {
            PreparedStatement statementCheck = con.prepareStatement(check);
            statementCheck.setString(1, tokens[1]);
            statementCheck.setString(2, currentPatient.getUsername());
            ResultSet resultCheck = statementCheck.executeQuery();
            if (resultCheck.next()) {
                // delete the appointment
                PreparedStatement statementCancel = con.prepareStatement(cancel);
                statementCancel.setString(1, tokens[1]);
                statementCancel.executeUpdate();
                // insert availabilities
                PreparedStatement statementInsert = con.prepareStatement(insert);
                statementInsert.setString(1, resultCheck.getString(5)); // Time
                statementInsert.setString(2, resultCheck.getString(3)); // Caregiver
                statementInsert.executeUpdate();
                // add dose
                Vaccine vaccine = new Vaccine.VaccineGetter(resultCheck.getString(4)).get();
                vaccine.increaseAvailableDoses(1);
                System.out.println("Canceled successfully!");
            }
            else {
                System.out.println("No such appointment!");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SQLException();
        } finally {
            cm.closeConnection();
        }
    }

    private static void addDoses(String[] tokens) {
        // add_doses <vaccine> <number>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String vaccineName = tokens[1];
        int doses = Integer.parseInt(tokens[2]);
        Vaccine vaccine = null;
        try {
            vaccine = new Vaccine.VaccineGetter(vaccineName).get();
        } catch (SQLException e) {
            System.out.println("Error occurred when adding doses");
            e.printStackTrace();
        }
        // check 3: if getter returns null, it means that we need to create the vaccine and insert it into the Vaccines
        //          table
        if (vaccine == null) {
            try {
                vaccine = new Vaccine.VaccineBuilder(vaccineName, doses).build();
                vaccine.saveToDB();
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
                e.printStackTrace();
            }
        } else {
            // if the vaccine is not null, meaning that the vaccine already exists in our table
            try {
                vaccine.increaseAvailableDoses(doses);
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
                e.printStackTrace();
            }
        }
        System.out.println("Doses updated!");
    }

    private static void showAppointments(String[] tokens) throws SQLException {
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first!");
            return;
        }
        else if (tokens.length != 1) {
            System.out.println("Please try again!");
            return;
        }
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        String name = "";
        String search = "";
        if (currentCaregiver != null) {
            name = currentCaregiver.getUsername();
            search = "SELECT A.Id, Vaccine, A.Time, A.Patient FROM Appointment A WHERE Caregiver = ? ORDER BY A.Id";
        }
        else if (currentPatient != null) {
            name = currentPatient.getUsername();
            search = "SELECT A.Id, Vaccine, A.Time, A.Caregiver FROM Appointment A WHERE Patient = ? ORDER BY A.Id";
        }
        try {
            PreparedStatement statement = con.prepareStatement(search);
            statement.setString(1, name);
            ResultSet result = statement.executeQuery();
            boolean hasAppointment = false;
            hasAppointment = result.next();
            if (hasAppointment) {
                do {
                    System.out.println("Appointment ID: " + result.getString(1));
                    System.out.println("Vaccine: " + result.getString(2));
                    System.out.println("Date: " + result.getString(3));
                    if (currentCaregiver != null) {
                        System.out.println("Caregiver: " + result.getString(4));
                    }
                    else {
                        System.out.println("Patient: " + result.getString(4));
                    }
                    System.out.println();
                } while (result.next());
            }
            else {
                System.out.println("No appointment!");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            throw new SQLException();
        } finally {
            cm.closeConnection();
        }
    }

    private static void logout(String[] tokens) {
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first!");
            return;
        }
        if (tokens.length != 1) {
            System.out.println("Please try again!");
            return;
        }
        currentPatient = null;
        currentCaregiver = null;
        System.out.println("Successfully logged out!");
        return;
    }

    private static boolean checkPasswordIllegal(String password) {
        boolean upperCase = false;
        boolean lowerCase = false;
        boolean letter = false;
        boolean digit = false;
        boolean specialChar = false;
        boolean length = false;
        if (password.length() >= 8) {
            length = true;
        }
        for (int i = 0; i < password.length(); i++) {
            if (Character.isUpperCase(password.charAt(i))) {
                upperCase = true;
            }
            if (Character.isLowerCase(password.charAt(i))) {
                lowerCase = true;
            }
            if (Character.isLetter(password.charAt(i))) {
                letter = true;
            }
            if (Character.isDigit(password.charAt(i))) {
                digit = true;
            }
        }
        if (password.contains("!") || password.contains("@") || password.contains("#") || password.contains("?")){
            specialChar = true;
        }
        return length && upperCase && lowerCase && letter && digit && specialChar;
//        return true;
    }
}
