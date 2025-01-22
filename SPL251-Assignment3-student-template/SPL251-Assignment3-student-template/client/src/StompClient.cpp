#include <stdlib.h>
#include "../include/ConnectionHandler.h"
#include "../include/keyboardInput.h"
#include "../include/keyboardInput.h"
#include "../include/netWorkThread.h"
#include <iostream>
#include <thread>
#include <mutex>

std::mutex mtx; // Mutex for synchronization between threads


int main(int argc, char *argv[]) {
    if (argc < 3) {
        std::cerr << "Usage: " << argv[0] << " host port" << std::endl;
        return -1;
    }

    std::string host = argv[1];
    short port = atoi(argv[2]);

    ConnectionHandler connectionHandler(host, port);
    if (!connectionHandler.connect()) {
        std::cerr << "Could not connect to server at " << host << ":" << port << std::endl;
        return -1;
    }

    bool isActive = true; // Shared flag to control both threads

    // Launch the keyboard and network threads
	keyboardInput runnable1();
	netWorkThread runnable2();

	//should be activated inside the keyboard probably
	std::thread keyBoardThread(&keyboardInput::run, &runnable1, std::ref(connectionHandler), std::ref(isActive));
    std::thread networkThread(&netWorkThread::run, &runnable2, std::ref(connectionHandler), std::ref(isActive), std::ref(mtx));

    // Wait for both threads to finish
    keyBoardThread.join();
    networkThread.join();

    connectionHandler.close(); // Ensure the connection is closed properly
    std::cout << "Client terminated gracefully." << std::endl;

    return 0;
    }
