import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class ProjectClient {

	private Socket clientSocket;
	private PrintWriter out;
	private BufferedReader in;

	public ProjectClient() {

	}

	public static void main(String[] args) throws IOException {
		ProjectClient client = new ProjectClient();
		
		try {
		client.startConnection("192.168.1.115", 6666);
		} catch(IOException e) {
			System.out.println("ERROR: CANNOT CONNECT TO SERVER. SYSTEM WILL NOW EXIT");
			System.exit(0);
		}
		
		System.out.println("CLIENT CONNECTED TO SERVER");

		// Below is the if tree for logging in, creating a new user, or exiting. There
		// are no unexpected messages here, thus we never have to rapid print out the
		// buffer
		boolean loggedIn = false;
		boolean exitCommand = false;
		Scanner keyboardIn = new Scanner(System.in);
		while (!loggedIn && !exitCommand) {

			System.out.println("WELCOME TO IM SERVICE");
			System.out.println("ENTER 1 TO LOGIN");
			System.out.println("ENTER 2 TO CREATE A NEW USER");
			System.out.println("ENTER 3 TO EXIT");

			String next = keyboardIn.nextLine();

			if (next.equals("1")) {
				System.out.println("LOGIN SELECTED");
				System.out.println("PLEASE ENTER USERNAME");
				String username = keyboardIn.nextLine();

				System.out.println("PLEASE ENTER PASSWORD");
				String password = keyboardIn.nextLine();

				client.sendMessage("login\n" + username + "\n" + password + "\nend\n");

				// adds a buffer so that the server has time to respond
				try {
					TimeUnit.SECONDS.sleep(1);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				loggedIn = client.receiveMessage();

				if (loggedIn) {
					System.out.println("SUCESSFULLY LOGGED IN");
				}

			} else if (next.equals("2")) {
				System.out.println("CREATE NEW USER SELECTED");
				System.out.println("PLEASE ENTER USERNAME");
				String username = keyboardIn.nextLine();

				System.out.println("PLEASE ENTER PASSWORD");
				String password = keyboardIn.nextLine();

				client.sendMessage("newUser\n" + username + "\n" + password + "\nend\n");

				// adds a buffer so that the server has time to respond
				try {
					TimeUnit.SECONDS.sleep(1);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				client.receiveMessage();

			} else if (next.equals("3")) {
				client.sendMessage("exit");
				exitCommand = true;
			} else {
				System.out.println("INVALID INPUT");
			}

		}

//This is the main menu. There could be multiple messages coming in from lots of other users when we are trying to get a specific anser
		while (!exitCommand) {
			System.out.println();
			System.out.println("MAIN MENU");
			System.out.println("ENTER 1 TO GET LIST OF ONLINE USERS");
			System.out.println("ENTER 2 TO SEND A MESSAGE TO A USER");
			System.out.println("ENTER 3 TO EXIT");
			System.out.println("ENTER 4 TO REFRESH MESSAGES");

			String next = keyboardIn.nextLine();
			for (int i = 0; i < 100; i++) {
				client.receiveMessage();
			}

			if (next.equals("1")) {
				System.out.println("GET LIST OF ONLINE USERS SELECTED");

				client.sendMessage("getUsers\nend\n");

				loggedIn = client.receiveMessage();

			} else if (next.equals("2")) {
				System.out.println("SEND A MESSAGE TO A USER SELECTED");
				System.out.println("PLEASE ENTER RECIPIANTS USERNAME");
				String username = keyboardIn.nextLine();

				System.out.println("PLEASE ENTER MESSAGE");
				String message = keyboardIn.nextLine();

				client.sendMessage("send\n" + username + "\n" + message + "\nend\n");

				// adds a buffer so that the server has time to respond
				try {
					TimeUnit.SECONDS.sleep(1);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				if (client.receiveMessage()) {
					System.out.println("MESSAGE SENT SUCCESSFULLY");
				}

			} else if (next.equals("3")) {

				client.sendMessage("exit");
				exitCommand = true;

			} else if (next.equals("4")) {
				for (int i = 0; i < 100; i++) {
					client.receiveMessage();
				}
			}

			else {
				System.out.println("INVALID INPUT");
			}
			// just read messages like a madman because I can assert that not more than 100
			// messages will be coming through
			// This essentially clears the read buffer so that when we are expecting a
			// message it will always be the first through
			for (int i = 0; i < 100; i++) {
				client.receiveMessage();
			}

		}

		keyboardIn.close();
		client.stopConnection();
	}

	//starts connection with server where ip is the ip of the server. 
	public void startConnection(String ip, int port) throws UnknownHostException, IOException {

		clientSocket = new Socket(ip, port);

		out = new PrintWriter(clientSocket.getOutputStream(), true);

		in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

	}

	// This sends a message to server
	public void sendMessage(String msg) {

		out.println(msg);

	}

	private boolean receiveMessage() throws IOException {
		// This parses the message received from the server and executes the
		// corresponding function
		String command;
		boolean messageValue = true;

		// if there is a message ready to be read then we handle it, otherwise do
		// nothing
		// This way we can use this as a general "check for messages" method or
		// "check for a message we know is coming" method (EX: we would know that a
		// login result is coming)
		if (in.ready()) {
			// our protocol dictates that the first line of a message is a command
			// the rest of the lines are just arguments for that command
			command = in.readLine();

			// we parse the first line to get the command and send it off to be handled
			if (command.equals("loginResult")) {
				messageValue = userLoginResult();

			} else if (command.equals("newUserResult")) {
				messageValue = userCreationResult();

			} else if (command.equals("messageResult")) {
				messageResult();

			} else if (command.equals("sendMessageResult")) {
				messageValue = sendMessageResult();

			} else if (command.equals("usersOnline")) {
				usersOnlineResult();

			}

		}
		return messageValue;
	}

	// This evaluates the message from server if a user was successfully created or
	// not.
	private boolean userCreationResult() throws IOException {
		boolean userCreated = false;

		if (in.readLine().equals("success")) {
			userCreated = true;
		}

		String next = in.readLine();
		while (!next.equals("end")) {
			System.out.println(next);
			next = in.readLine();
		}

		return userCreated;
	}

	// This evaluates the message from server if a user was successfully logged in
	// or not.
	private boolean userLoginResult() throws IOException {
		boolean userLogin = false;

		if (in.readLine().equals("success")) {
			userLogin = true;
		}

		String next = in.readLine();
		while (!next.equals("end")) {
			System.out.println(next);
			next = in.readLine();
		}

		return userLogin;
	}

	// This evaluates/prints the message from server detailing the number of users
	// online + who was online
	private void usersOnlineResult() throws IOException {
		String numOfUsers = in.readLine();

		System.out.println("Number of Users Online: " + numOfUsers);
		System.out.println("Users online:");

		String next = in.readLine();
		while (!next.equals("end")) {
			System.out.println(next);
			next = in.readLine();
		}

	}

	// This evaluates the servers response to client sending a message
	// True if message was sent successfully
	// False if error occurred, and this would return an error message

	private boolean sendMessageResult() throws IOException {
		boolean messageSent = false;

		// reads either fail or success
		if (in.readLine().equals("success")) {
			messageSent = true;
		}

		// reads either error message or end
		String next = in.readLine();
		while (!next.equals("end")) {
			System.out.println(next);
			next = in.readLine();
		}

		return messageSent;
	}

	// This prints a message from another user
	private void messageResult() throws IOException {

		String sender = in.readLine();
		System.out.println("New Message From: " + sender);

		String next = in.readLine();
		while (!next.equals("end")) {
			System.out.println(next);
			next = in.readLine();
		}

	}

	// stops the clients connection to server by closing buffered reader, print
	// writer, and socket
	public void stopConnection() {
		try {
			in.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		out.close();

		try {
			clientSocket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		System.out.println("Connection with server closed");
	}
}
