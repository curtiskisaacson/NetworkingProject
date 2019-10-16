import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

public class ProjectServer {

	private ServerSocket serverSocket;

	private static Map<String, ProjectClientHandler> onlineUsers;
	private static Map<String, String> database;

	public ProjectServer() {
		onlineUsers = new HashMap<>();
		database = new HashMap<>();

	}

	public static void main(String[] args) throws IOException {
		ProjectServer server = new ProjectServer();
		server.start(6666);

		server.stop();
	}

	// adds all users in database to database map of user names and passwords
	private void buildDatabase() {

		File data = new File("src/database");
		Scanner databaseIn = null;
		try {
			databaseIn = new Scanner(data);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		String next;
		String username;
		String password;
		while (databaseIn.hasNext()) {
			next = databaseIn.nextLine();

			int locationOfComma = 0;
			while (next.charAt(locationOfComma) != ',') {
				locationOfComma++;
			}

			username = next.substring(0, locationOfComma);
			password = next.substring(locationOfComma + 1, next.length());

			ProjectServer.database.put(username, password);

		}

	}

	public void start(int port) {

		buildDatabase();
		try {
			serverSocket = new ServerSocket(port);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		try {
			System.out.println("Waiting on clients on "+serverSocket.getInetAddress().getLocalHost().getHostAddress()+":"+port);
		} catch (UnknownHostException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		while (true) {
			// waits for accept call, then starts new thread of a ProjectUser
			try {
				new ProjectClientHandler(serverSocket.accept()).start();
				System.out.println("NEW CLIENT CONNECTED");
				
			} catch(IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void stop() {
		try {
			serverSocket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static class ProjectClientHandler extends Thread {
		private Socket clientSocket;
		// prints out to client
		private PrintWriter out;
		// reads input in from client
		private BufferedReader in;

		// this is the username associated with this thread. This will only get assigned
		// when login is successful
		private String clientUsername;

		public ProjectClientHandler(Socket socket) {
			this.clientSocket = socket;

		}

		// appends a new user to database list and database file. DOES NOT MARK THEM AS
		// ONLINE
		private void addUserToDatabase(String username, String password) {
			ProjectServer.database.put(username, password);

			File data = new File("src/database");
			FileWriter fw= null;
			
			try {
				fw = new FileWriter(data,true);
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			PrintWriter databaseOut = null;
			databaseOut = new PrintWriter(fw);
			databaseOut.println(username + "," + password);
			
			databaseOut.close();
			
			try {
				fw.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			

		}

		public void run() {

			// sets the output to user
			try {
				out = new PrintWriter(clientSocket.getOutputStream(), true);
			} catch (IOException e3) {
				// TODO Auto-generated catch block
				e3.printStackTrace();
			}

			// sets the input from user reader
			try {
				in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			} catch (IOException e3) {
				// TODO Auto-generated catch block
				e3.printStackTrace();
			}

			// This parses the message received from the client and executes the
			// corresponding function
			String command;
			boolean cont = true;
			try {
				// while we havent hit the exit command for a user
				while (cont) {
					// wait for a message from a user to come through
					while (!in.ready()) {

					}
					// our protocol dictates that the first line of a message is a command
					// the rest of the lines are just arguments for that command
					command = in.readLine();
					// we parse the first line to get the command and send it off to be handled
					if (command.equals("getUsers")) {
						System.out.println("getUsers command received from "+clientUsername);
						// This reads and drops the "end" keyword that we know is at the end of the
						// message
						in.readLine();
						parseGetUsersMessage();
					} else if (command.equals("send")) {
						// this command is for a message from client to be sent to another user
						System.out.println("send command received from"+clientUsername);
						parseSendMessage();
					} else if (command.equals("newUser")) {
						System.out.println("newUser command received");
						parseNewUserMessage();
					} else if (command.equals("login")) {
						System.out.println("login command received");
						parseLoginMessage();
					} else if (command.equals("exit")) {
						if(clientUsername.equals(null)) {
							System.out.println("exit command received from unknown user ");
						}else {
						System.out.println("exit command received from "+clientUsername);
						}
						cont = false;
						parseExitMessage();
					}
				}
			} catch (IOException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			}

		}

//This sends the client the list of users online and how many are online
// will print at least one user: yourself!
		private void parseGetUsersMessage() {

			out.println("usersOnline");
			out.println(ProjectServer.onlineUsers.size() + "\n");

			for (String user : ProjectServer.onlineUsers.keySet()) {
				out.println(user);
			}
			out.println("end");
		}

		// This parses a message from client that is inteneded for another user
		private void parseSendMessage() throws IOException {

			// gets recipiants username
			String username = null;
			try {
				username = in.readLine();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			// checks if the recipiant is online, if so, sends them the message and responds
			// to sender with a success, else responds to sender with a failure
			if (ProjectServer.onlineUsers.containsKey(username)) {
				String message = "";
				String next = in.readLine();
				while (!next.equals("end")) {
					message = message + next;
					next = in.readLine();					
				}
				
				out.println("sendMessageResult");
				out.println("success");
				out.println("end");
				
				//sends message to user
				ProjectServer.onlineUsers.get(username).out
				.println("messageResult\n" + this.clientUsername + "\n" + message + "\nend\n");
				
				System.out.println("Message successfully sent to "+username+" from "+this.clientUsername+": "+message);
				
			} else {
				out.println("sendMessageResult");
				out.println("fail");
				out.println("User " + username + " is not online or does not exist");
				out.println("end");
			}

		}

		private void parseNewUserMessage() {

			String username = null;
			try {
				username = in.readLine();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			String password = null;
			try {
				password = in.readLine();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			out.println("newUserResult");
			// check username does not exist
			if (ProjectServer.database.containsKey(username)) {
				out.println("fail");
				out.println("Username already exists");
				out.println("end");
			} else {
				out.println("success");
				out.println("end");
				addUserToDatabase(username, password);
			}

		}

		private void parseLoginMessage() {
			String username = null;
			try {
				username = in.readLine();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			String password = null;
			try {
				password = in.readLine();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			// check if user exists in database

			if (ProjectServer.database.containsKey(username)) {

				// check if user password combo is correct
				if (ProjectServer.database.get(username).equals(password)) {

					// check if user is online
					if (!ProjectServer.onlineUsers.containsKey(username)) {

						out.println("loginResult");
						out.println("success");
						out.println("end");

						this.clientUsername = username;
						ProjectServer.onlineUsers.put(username, this);
						System.out.println(username+" successfully logged in");

					} else {
						out.println("loginResult");
						out.println("fail");
						out.println("User is already online!");
						out.println("end");

					}

				} else {
					out.println("loginResult");
					out.println("fail");
					out.println("Incorrect password for user " + username);
					out.println("end");
				}

			} else {
				out.println("loginResult");
				out.println("fail");
				out.println("Username " + username + " does not exist.");
				out.println("end");
			}

		}

		private void parseExitMessage() {
			// TODO send a message to user saying goodbye
			// closes input and output
			try {
				in.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			out.close();
			// closes client socket connection
			try {
				clientSocket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			// checks if the user is logged in, if they are removes them from online
			if (this.clientUsername != "") {
				ProjectServer.onlineUsers.remove(this.clientUsername);
			}

			System.out.println("Connection with client closed");
		}
	}
}
