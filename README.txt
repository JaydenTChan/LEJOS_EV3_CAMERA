// ==== BOOF CV Example Java for LEJOS EV3

EV3_CAM contains the project and class file to use to communicate with the Camera Tracker.
This project should be compiled using eclipse and pushed onto the EV3 system.

	To use the program currently blocks using in.readUTF() but may be built to be
	asynchronous with in.available(). This file only contains the necessary components
	to receive data from JAVA_BOOFCV and no math, logic or algorithm to solve any
	problems.

JAVA_BOOFCV contains the camera tracker itself and should be compiled using gradle by typing
gradle webcamRun

	To use just drag your cursor across the end effector.
	Change mode to target mode and click a space for a target.

	Using the implemented tracker program it will send the following data when send is pushed:
	- Where the end effector is
	- Where the target is

	If this program is sent "check" as a string (without quotations) it will once again send the
	end effector position to the EV3

	All positional output from this program is pixel measurement from the webcam image.

