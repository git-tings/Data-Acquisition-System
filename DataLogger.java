import java.awt.BorderLayout;
import java.io.PrintStream;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

public class DataLogger extends JPanel{
	private static final long serialVersionUID = 1L;
	private JTextArea textArea;
	private JScrollPane scroller;
	private PrintStream printStream;
	
	public DataLogger () {
		//super();
		textArea = new JTextArea();
		scroller = new JScrollPane(textArea);
		printStream = new PrintStream(new TextAreaOutputStream(textArea));
		this.setLayout(new BorderLayout());
		this.setBorder(new TitledBorder (new EtchedBorder(), "Data Logger"));
		scroller.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		this.add(scroller);
		printStream.println("ROLL\tPITCH\tALT\tSPEED\tDROP");
	}
	
	public void printData (FlightDataPacket fdp) {
		printStream.println(fdp.getRoll() + "\t" + fdp.getPitch() + "\t" + fdp.getAlt() + "\t" + fdp.getSpeed());
	}
	
	public void printData(double roll, double pitch, double alt, double airspeed) {
		printStream.println(roll + "\t" + pitch + "\t" + alt + "\t" + airspeed);
	}
	
	public String getText() {
		return textArea.getText();
	}
	
}
