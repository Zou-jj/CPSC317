
// You can use this file as a starting point for your dictionary client
// The file contains the code for command line parsing and it also
// illustrates how to read and partially parse the input typed by the user. 
// Although your main class has to be in this file, there is no requirement that you
// use this template or hav all or your classes in this file.

import java.awt.print.Printable;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.System;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.io.BufferedReader;
import java.util.List;

//
// This is an implementation of a simplified version of a command
// line dictionary client. The only argument the program takes is
// -d which turns on debugging output.
//


public class CSdict {
    static final int MAX_LEN = 255;
    static Boolean debugOn = false;

    private static final int PERMITTED_ARGUMENT_COUNT = 1;
    private static String command;
    private static String[] arguments;

    public static void main(String[] args) {
        byte cmdString[] = new byte[MAX_LEN];
        boolean isRunning = true;
        boolean socketIsConnected = false;
        Socket socket = null;
        String setQuery = "";
        List<String> listOfDictionary = new ArrayList<>();
        String listOfDictionaryStatus = null;
        PrintWriter output = null;
        BufferedReader input = null;
        int len;

        String e900 = "900 Invalid command.";
        String e901 = "901 Incorrect number of arguments.";
        String e902 = "902 Invalid argument.";
        String e903 = "903 Supplied command not expected at this time.";
        String e925 = "925 Control connection I/O error, closing control connection.";
        String e996 = "996 Too many command line options - Only -d is allowed.";
        String e997 = "997 Invalid command line option - Only -d is allowed";
        String e998 = "998 Input error while reading commands, terminating.";
        String e999 = "999 Processing error. ";
        // Verify command line arguments
        if (args.length == PERMITTED_ARGUMENT_COUNT) {
            debugOn = args[0].equals("-d");
            if (debugOn) {
                System.out.println("Debugging output enabled");
            } else {
                System.out.println(e997);
                return;
            }
        } else if (args.length > PERMITTED_ARGUMENT_COUNT) {
            System.out.println(e996);
            return;
        }


        // Example code to read command line input and extract arguments.
        while (true) {
            try {
                System.out.print("csdict> ");
                System.in.read(cmdString);

                // Convert the command string to ASII
                String inputString = new String(cmdString, "ASCII");

                // Split the string into words
                String[] inputs = inputString.trim().split("( |\t)+");
                // Set the command
                command = inputs[0].toLowerCase().trim();
                // Remainder of the inputs is the arguments.
                arguments = Arrays.copyOfRange(inputs, 1, inputs.length);

                len = arguments.length;

                switch (command) {
                    case "open":
                        if (arguments.length != 2) {
                            System.out.println(e901);
                            break;
                        } if (!socketIsConnected) {
                        String serverName = arguments[0];
                        // Open socket and store it into a variable for other commands -- https://docs.oracle.com/javase/tutorial/networking/sockets/readingWriting.html
                        try {
                            int port = Integer.parseInt(arguments[1]);
                            socket = new Socket(serverName, port);
                            socketIsConnected = true;
                            // Every time a connection to a dictionary server is made the dictionary to use is reset to "*".
                            setQuery = "*";
                            // Adding all the dictionary and caching the database allows other commands to use the database without calling SET command
                             output = new PrintWriter(socket.getOutputStream(), true);
                             input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                             // Send command to
                            output.println("SHOW DB");
                            String serverResponse;
                            while (!(serverResponse = input.readLine()).equals(".")) {
                                listOfDictionary.add(serverResponse);
                            }
                            listOfDictionaryStatus = input.readLine();
                        } catch (NumberFormatException e) {
                            System.out.println(e902);
                            break;
                        } catch (Exception e) {
                            System.out.println("920 Control connection to " + serverName + " on port " + arguments[1] + " failed to open");
                        }
                    } else {
                        System.out.println(e903);
                    }
                        break;
                    case "dict":
                        if (arguments.length != 0) {
                            System.out.println(e901);
                            break;
                        }
                        if (socketIsConnected && !socket.equals(null)) {
                            try {
                                for (String database : listOfDictionary) {
                                    System.out.println(database);
                                }
                                if (debugOn) {
                                    System.out.println( "<-- " + listOfDictionaryStatus);
                                }
                            } catch (Exception e) {
                                System.out.println(e925);
                                socket = null;
                                socketIsConnected = false;
                            }
                        } else {
                            System.out.println(e903);
                        }
                        break;
                    case "set":
                        if (arguments.length > 1) {
                            System.out.println(e901);
                            break;
                        }
                        if (socketIsConnected) {
                            // Need to clarify with "!" string
                                if (arguments.length == 0 || arguments[0].equals("*")) {
                                    setQuery = "*";
                                } else {
                                    setQuery = arguments[0];
                                }
                        } else {
                            System.out.println(e903);
                        }
                        break;
                    case "define":
                        if (arguments.length != 1) {
                            System.out.println(e901);
                            break;
                        }
                        if (!socketIsConnected) {
                            System.out.println(e903);
                            break;
                        }
                        try {
                            String command = "DEFINE " + setQuery + " " + arguments[0];
                           if (debugOn) {
                               System.out.println(command);
                           }
                           String serverResponse = "";
                            output.println(command);
                            while ((serverResponse = input.readLine())!= null) {
                                if (serverResponse.contains("552")) {
                                    if (debugOn) {
                                        System.out.println(serverResponse);
                                    }
                                    System.out.println("****No definition found****");
                                    break;
                                } if (serverResponse.contains("550")) {
                                    if (debugOn) {
                                        System.out.println(serverResponse);
                                    }
                                    System.out.println("****No matches found****");
                                    break;
                                } else {
                                    if (serverResponse.contains("150")) {
                                        if (debugOn) {
                                            System.out.println("<-- " + serverResponse);
                                        }
                                    } else if (serverResponse.contains("151")) {
                                        // To retrieve the dictionary name, we must remove the length of the name, 3 spaces and 3 quotation
                                        int shortenStatus = 6 + arguments[0].length();
                                        System.out.println("@" + serverResponse.substring(shortenStatus));
                                    } else if (serverResponse.contains("250")) {
                                        if (debugOn) {
                                            System.out.println(" <-- " + serverResponse);
                                        }
                                        break;
                                    } else {
                                        System.out.println(serverResponse);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            System.out.println(e925);
                            socket = null;
                            input.close();
                            output.close();
                            socketIsConnected = false;
                        }
                        break;
                    case "match":
                        if (arguments.length != 1) {
                            System.out.println(e901);
                            break;
                        }
                        if (!socketIsConnected) {
                            System.out.println(e903);
                            break;
                        }
                        try {
                            String command = "MATCH " + setQuery + " exact " + arguments[0];
                           if (debugOn) {
                               System.out.println(command);
                           }
                           String serverResponse = "";
                            output.println(command);
                            while ((serverResponse = input.readLine())!= null) {
                                if (serverResponse.contains("552")) {
                                    if (debugOn) {
                                        System.out.println(serverResponse);
                                    }
                                    System.out.println("****No definition found****");
                                    break;
                                } if (serverResponse.contains("550")) {
                                    if (debugOn) {
                                        System.out.println(serverResponse);
                                    }
                                    System.out.println("****No matches found****");
                                    break;
                                } else {
                                    if (serverResponse.contains("150")) {
                                        if (debugOn) {
                                            System.out.println("<-- " + serverResponse);
                                        }
                                    } else if (serverResponse.contains("151")) {
                                        // To retrieve the dictionary name, we must remove the length of the name, 3 spaces and 3 quotation
                                        int shortenStatus = 6 + arguments[0].length();
                                        System.out.println("@" + serverResponse.substring(shortenStatus));
                                    } else if (serverResponse.contains("250")) {
                                        if (debugOn) {
                                            System.out.println(" <-- " + serverResponse);
                                        }
                                        break;
                                    } else {
                                        System.out.println(serverResponse);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            System.out.println(e925);
                            socket = null;
                            input.close();
                            output.close();
                            socketIsConnected = false;
                        }
                        break;
                    case "prefixmatch":
                        if (arguments.length != 1) {
                            System.out.println(e901);
                            break;
                        }
                        if (!socketIsConnected) {
                            System.out.println(e903);
                            break;
                        }
                        try {
                            String command = "MATCH " + setQuery + " prefix " + arguments[0];
                           if (debugOn) {
                               System.out.println(command);
                           }
                           String serverResponse = "";
                            output.println(command);
                            while ((serverResponse = input.readLine())!= null) {
                                if (serverResponse.contains("552")) {
                                    if (debugOn) {
                                        System.out.println(serverResponse);
                                    }
                                    System.out.println("****No definition found****");
                                    break;
                                } if (serverResponse.contains("550")) {
                                    if (debugOn) {
                                        System.out.println(serverResponse);
                                    }
                                    System.out.println("****No matches found****");
                                    break;
                                } else {
                                    if (serverResponse.contains("150")) {
                                        if (debugOn) {
                                            System.out.println("<-- " + serverResponse);
                                        }
                                    } else if (serverResponse.contains("151")) {
                                        // To retrieve the dictionary name, we must remove the length of the name, 3 spaces and 3 quotation
                                        int shortenStatus = 6 + arguments[0].length();
                                        System.out.println("@" + serverResponse.substring(shortenStatus));
                                    } else if (serverResponse.contains("250")) {
                                        if (debugOn) {
                                            System.out.println(" <-- " + serverResponse);
                                        }
                                        break;
                                    } else {
                                        System.out.println(serverResponse);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            System.out.println(e925);
                            socket = null;
                            input.close();
                            output.close();
                            socketIsConnected = false;
                        }
                        break;
                    case "close":
                        if (arguments.length != 0) {
                            System.out.println(e901);
                            break;
                        }
                        if (socketIsConnected) {
                            try {
                                socketIsConnected = false;
                                listOfDictionary.clear();
                            } catch (NumberFormatException e) {
                                System.out.println(e902);
                                break;
                            } catch (Exception e) {
                                System.out.println("920 Control connection failed to close");
                            }
                        } else {
                            System.out.println(e903);
                        }
                        break;
                    case "quit":
                        System.exit(0);
                        break;
                    default:
                        System.out.println(e900);
                }
                // clear commandline string
                Arrays.fill(cmdString, (byte) 0);
            } catch (IOException exception) {
                System.err.println(e998);
                System.exit(-1);
            }
        }
    }


}
    
    
