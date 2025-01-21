#include <iostream>
#include <string>
#include <sstream>
#include "ConnectionHandler.h"
#include "keyboardInput.h"
class keyboardThread{

    public:
    void run(ConnectionHandler &connectionHandler, bool &isActive) {
       int recipt_id = 0;
        while (isActive) {
        std::string userInput;
        std::getline(std::cin, userInput); // Read user input

        std::stringstream ss(userInput);
        std::string command;
        ss >> command;

        if (command == "login") {
            std::string hostPort, username, password;
            ss >> hostPort >> username >> password;

            std::string frame = "CONNECT\naccept-version:1.2\n";
            frame += "host:stomp.cs.bgu.ac.il\n";
            frame += "login:" + username + "\n";
            frame += "passcode:" + password + "\n";
            frame += '\0'; // Null character as the delimiter

            if (!connectionHandler.sendLine(frame)) {
                std::cerr << "Failed to send login frame to server." << std::endl;
                isActive = false;
            }
        } else if (command == "join") {
            std::string channelName;
            ss >> channelName;

            static int subscriptionId = 1; // Unique ID for subscriptions
            std::string frame = "SUBSCRIBE\ndestination:/" + channelName + "\n";
            frame += "id:" + std::to_string(subscriptionId++) + "\n";
            frame += "receipt:"+std::to_string(recipt_id)+"\n";
            frame += '\0';
            recipt_id+=1;

            if (!connectionHandler.sendLine(frame)) {
                std::cerr << "Failed to send subscribe frame." << std::endl;
                isActive = false;
            }
        } else if (command == "logout") {
            std::string frame = "DISCONNECT\n"+std::to_string(recipt_id)+"\n";
            frame += '\0';
            recipt_id+=1;


            if (!connectionHandler.sendLine(frame)) {
                std::cerr << "Failed to send disconnect frame." << std::endl;
            }

            isActive = false;
        } 

        else if (command == "exit") {//unsubscribe
        }

        else if (command == "summary") {
        }
        
        else if (command == "report") {//inside here send also
        }
        else {
            std::cerr << "Unknown command: " << command << std::endl;
        }
    }
    }
};