#include <iostream>
#include <string>
#include <sstream>
#include <mutex>
#include "ConnectionHandler.h"
#include "keyboardInput.h"
class netWorkThread{
    public:
    void run(ConnectionHandler &connectionHandler, bool &isActive, std::mutex mtx) {
    while (isActive) {
        std::string serverMessage;

        // Read messages from the server
        if (!connectionHandler.getLine(serverMessage)) {
            std::lock_guard<std::mutex> lock(mtx);
            std::cerr << "Connection to server lost. Exiting..." << std::endl;
            isActive = false;
            break;
        }

        // Handle server messages
        {
            std::lock_guard<std::mutex> lock(mtx);
            if (serverMessage.find("RECEIPT") != std::string::npos) {
                std::cout << "Server Receipt: " << serverMessage << std::endl;
            } else if (serverMessage.find("MESSAGE") != std::string::npos) {
                std::cout << "Message from server: " << serverMessage << std::endl;
            } else if (serverMessage.find("ERROR") != std::string::npos) {
                std::cerr << "Server Error: " << serverMessage << std::endl;
                isActive = false;
            } else {
                std::cout << "Unknown server response: " << serverMessage << std::endl;
            }
        }
    }
}