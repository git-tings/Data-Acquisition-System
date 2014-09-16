import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.TooManyListenersException;


public class ControlPanel extends JPanel implements SerialPortEventListener {
	
	private JPanel top;
	private JPanel middle1;
	private JPanel middle2;
	private JPanel bottom;
	
	private JButton btnCommPortRefresh;
	private JButton btnCommPortConnect;
	private JButton btnCommPortDisconnect;
	private JButton btnDropPackage;
	private JButton btnSaveData;
	
	private JComboBox<String> cmboxCommPorts;
	private DataLogger dl;
	
	private HashMap<String, CommPortIdentifier> portMap;
	private CommPortIdentifier selectedPortIdentifier;
	private Enumeration portList;
	private SerialPort serialPort;
	private boolean connected = false;
    private InputStream input;
    private OutputStream output;
	final static int timeout = 2000;
	private StringBuffer received;
	private int charCount = 0;
	
	public ControlPanel(DataLogger _dl) {
		super();
		dl = _dl;
		initializeGUIComponents();
		this.setBorder(new TitledBorder (new EtchedBorder(), "Control Panel"));
		this.setLayout(new GridLayout(4, 0, 0, 0));
    	portMap = new HashMap<String, CommPortIdentifier>();
    	received = new StringBuffer();
    	updatePortList();
	}

	
	private void initializeGUIComponents() {
		cmboxCommPorts = new JComboBox<String>();		
		
		btnCommPortRefresh = new JButton("Refresh");
		btnCommPortRefresh.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				updatePortList();
			}
		});
		
		btnCommPortConnect = new JButton("Connect");
		btnCommPortConnect.setEnabled(true);
		btnCommPortConnect.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				connectSerial();
				if (connected) {
					if (initStreams() == true) {
						initEventListener();
					}
				}
			}
		});
		
		btnCommPortDisconnect = new JButton("Disconnect");
		btnCommPortDisconnect.setEnabled(false);
		btnCommPortDisconnect.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				disconnectSerial();
			}
		});
		
		btnDropPackage = new JButton("Drop ");
		btnDropPackage.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.out.println("Package dropped");
			}
		});
		
		btnSaveData = new JButton("Save");
		btnSaveData.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
                JFileChooser saveFile = new JFileChooser();
                saveFile.addChoosableFileFilter(new FileNameExtensionFilter("Text File (*.txt)", ".txt"));
                saveFile.setFileSelectionMode(JFileChooser.FILES_ONLY);
                int val = saveFile.showSaveDialog(null);
                if (val == JFileChooser.APPROVE_OPTION) {                		
                	String filename = saveFile.getSelectedFile().toString();
                	try {
                	if (saveFile.getFileFilter().getDescription().equals("Text File (*.txt)"))
                		filename += ".txt";
                	} catch (Exception err) {}
                	FileWriter fstream = null;
                	BufferedWriter writer = null;
                	String data = dl.getText();
                	System.out.println("Attempting to save data as \"" + filename + "\"");
                	try {
                		fstream = new FileWriter(filename);
                		writer = new BufferedWriter(fstream);
						writer.write(data);
						writer.close();
						System.out.println("File saved succesfully.");
                	} catch (IOException err) {
                		System.out.println("Error saving file.");
                		err.printStackTrace();
                	}
                }
			}
		});
		
		
		top = new JPanel(new FlowLayout(FlowLayout.LEADING));
		middle1 = new JPanel(new FlowLayout(FlowLayout.LEADING));
		middle2 = new JPanel(new FlowLayout(FlowLayout.LEADING));
		bottom = new JPanel(new FlowLayout(FlowLayout.LEADING));
		
		top.setBorder(new TitledBorder(new EtchedBorder(), "Comm. Port"));
		middle1.setBorder(new TitledBorder(new EtchedBorder(), "Controls"));
		middle2.setBorder(new TitledBorder(new EtchedBorder(), "Data Recording"));
		bottom.setBorder(new TitledBorder(new EtchedBorder(), "Console"));
		
		top.add(cmboxCommPorts);
		top.add(btnCommPortRefresh);
		top.add(btnCommPortConnect);
		top.add(btnCommPortDisconnect);
		middle1.add(btnDropPackage);
		middle2.add(btnSaveData);
		
		JTextArea consoleTextArea = new JTextArea();
		JScrollPane scroller = new JScrollPane(consoleTextArea);
		PrintStream console = new PrintStream(new TextAreaOutputStream(consoleTextArea));
		bottom.setLayout(new BorderLayout());
		scroller.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		bottom.add(scroller);
		System.setOut(console);
		System.setErr(console);
				
		this.add(top);
		this.add(middle1);
		this.add(middle2);
		this.add(bottom);
	}
	
	private void updatePortList() {
    	System.out.println("Available Comm. Ports:");
    	portList = CommPortIdentifier.getPortIdentifiers();
    	cmboxCommPorts.removeAllItems();
    	while(portList.hasMoreElements()) {
    		CommPortIdentifier currPort = (CommPortIdentifier)portList.nextElement();
    		if (currPort.getPortType() == CommPortIdentifier.PORT_SERIAL) {
    			System.out.println(currPort.getName());
    			portMap.put(currPort.getName(), currPort);
    			cmboxCommPorts.addItem(currPort.getName());
    		}
    	}
    }

	private void connectSerial() {
    	received.setLength(0);
    	String selectedPort = cmboxCommPorts.getSelectedItem().toString();
    	selectedPortIdentifier = (CommPortIdentifier)portMap.get(selectedPort);
    	CommPort commPort;
    	try {
    		commPort = selectedPortIdentifier.open(this.getClass().getName(), timeout);
    		serialPort = (SerialPort) commPort;
    		connected = true;
    		btnCommPortConnect.setEnabled(false);
    		btnCommPortDisconnect.setEnabled(true);
    		System.out.println(selectedPort + " opened succesfully.");
    	} catch (Exception e) {
    		System.out.println("Failed to connect to " + selectedPort);
    		e.printStackTrace();
    	}
    }
    
    private void disconnectSerial() {
    	received.setLength(0); //empties the buffer
    	try {
    		serialPort.removeEventListener();
    		serialPort.close();
    		connected = false;
    		btnCommPortConnect.setEnabled(true);
    		btnCommPortDisconnect.setEnabled(false);
    		System.out.println("Disconnect from " + serialPort.getName());
    	} catch (Exception e) {
    		System.out.println("Failed to close " + serialPort.getName());
    		e.printStackTrace();
    	}
    }

    private boolean initStreams () {
    	try {
    		input = serialPort.getInputStream();
    		output = serialPort.getOutputStream();
    		return true;
    	} catch (Exception e) {
    		System.out.println("Failed to initialize IO streams.");
    		e.printStackTrace();
    	}
    	return false;
    }

    private void initEventListener() {
        try {
            serialPort.addEventListener(this);
            serialPort.notifyOnDataAvailable(true);
        }
        catch (TooManyListenersException e) {
            System.out.println("Too many listeners: " + e.toString());
        }
    }
 

    public void serialEvent(SerialPortEvent evt) {
        if (evt.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
            try {
                byte singleData = (byte)input.read();
                received.append(new String(new byte[] {singleData}));
                //System.out.println(received);
                charCount++;
                if (charCount>10) {
                	extractPacket();
                	charCount = 0;
                }
            }
            catch (Exception e) {
                System.out.println("Failed to read data.");
                e.printStackTrace();
            }
        }
    }
    
    private void extractPacket () {
    	String str;
    	String temp = received.toString();
    	if (temp.contains("*") && temp.contains("&")) {
    		if (temp.indexOf("*") < temp.indexOf("&")) {
    			str = temp.substring(temp.indexOf("*")+1, temp.indexOf("&"));
    			temp = temp.substring(temp.indexOf("&")+1, temp.length());
    			dl.printData(analyzePacket(str));
    		}
    	}
		else {
			temp = temp.substring(temp.indexOf("*"));
		}
		received.setLength(0);
		received.append(temp);
    }

    private void writeData() {
     //fill this in eventually
    }
    
    private FlightDataPacket analyzePacket (String str) {
		String [] strArr = str.split("%");
		double [] dblArr = new double [4];
		
		try {
			for (int i = 0; i < 4; i++) {
				dblArr[i] = Double.parseDouble(strArr[i]);
				dblArr[i] *= 100;
				dblArr[i] = Math.round(dblArr[i]) / 100.0;
			}
		} catch (Exception e) {
			System.out.println("Encountered an invalid packet: \"" + str + "\"");
		}
		//packet format: *roll%pitch%altitude%speed%1 character&
		return new FlightDataPacket(dblArr[0], dblArr[1], dblArr[2], dblArr[3], strArr[4]);
	}

}
