CREATE TABLE Caregivers (
    Username varchar(255),
    Salt BINARY(16),
    Hash BINARY(16),
    PRIMARY KEY (Username)
);

CREATE TABLE Availabilities (
    Time date,
    Username varchar(255) REFERENCES Caregivers,
    PRIMARY KEY (Time, Username)
);

CREATE TABLE Vaccines (
    Name varchar(255),
    Doses int,
    PRIMARY KEY (Name)
);

CREATE TABLE Patient (
    Username varchar(255) primary key,
    Salt binary(16),
    Hash binary(16)
);

CREATE TABLE Appointment (
    Id int primary key,
    Patient varchar(255) REFERENCES Patient,
    Caregiver varchar (255) REFERENCES Caregivers,
    Vaccine varchar(255) REFERENCES Vaccines,
    Time date REFERENCES Availabilities
);