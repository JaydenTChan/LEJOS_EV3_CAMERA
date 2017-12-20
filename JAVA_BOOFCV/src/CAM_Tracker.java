/*
 * Source From: https://github.com/lessthanoptimal/TutorialObjectTracking
 *
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://boofcv.org).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import boofcv.abst.tracker.TrackerObjectQuad;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.factory.tracker.FactoryTrackerObjectQuad;
import boofcv.io.webcamcapture.UtilWebcamCapture;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.ImageUInt8;
import boofcv.struct.image.MultiSpectral;
import com.github.sarxos.webcam.Webcam;
import georegression.geometry.UtilPolygons2D_F64;
import georegression.struct.point.Point2D_I32;
import georegression.struct.shapes.Quadrilateral_F64;
import georegression.struct.shapes.RectangleCorner2D_F64;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;

// === Added imports
import java.awt.geom.*;
import java.text.DecimalFormat;
import java.awt.event.*;

//Network Imports
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Demonstration for how to open a webcam and track an object in the video feed selected by the user.  To select an object
 * simply click and drag a rectangle across.  To change which tracker is used comment/uncomment the code in main().
 *
 * Code originally from BoofCV's WebcamCapture examples.
 *
 * @author Peter Abeles
 */
public class CAM_Tracker<T extends ImageBase> extends JPanel
		implements MouseListener, MouseMotionListener {

	TrackerObjectQuad<T> tracker;

	// location of the target being tracked
	Quadrilateral_F64 target = new Quadrilateral_F64();

	// location selected by the mouse
	Point2D_I32 point0 = new Point2D_I32();
	Point2D_I32 point1 = new Point2D_I32();
    
    Point2D_I32 target1 = new Point2D_I32();

	int desiredWidth,desiredHeight;
	volatile int mode = 0;

	BufferedImage workImage;

    // === Swing
	JFrame window;
    JFrame cpanel;
    
    JPanel stats;
    JPanel buttons;
    JPanel network;

    JLabel status, realCoord, relaCoord, conStatus, portL, ipL, targetCoord, receive;
    
    JButton show, selectTarget, selectObject, removeTarget, Connect, sendCom;
    
    JTextField ipF, portF;
    
    // === Variables
    int modeSwitch = 0; // 0 = Object, 1 = target
    
    boolean shown = true;
    boolean tar = false;
    boolean connected = false;
    
    double x_calc = 0;
    double y_calc = 0;
    
    String response;
    double[] send;
    
    // === Constants
    int origin_x = 320;
    int origin_y = 420;
    
    double pixel_to_cm = 17.98; //17.98 pixels per cm
    
    // === Networking
	String ip = "10.0.1.8";     //IP of EV3
	int port = 1112;            //Port to connect to
    public static Socket sock;
	DataOutputStream out;
    DataInputStream in;
    
    DecimalFormat df = new DecimalFormat("00.00"); //https://stackoverflow.com/questions/433958/java-decimal-string-format
    
    
	/**
	 * Configures the tracking application
	 *
	 * @param tracker The object tracker
	 * @param desiredWidth Desired size of the input stream
	 * @param desiredHeight Desired height of the input stream
	 */
	public CAM_Tracker(TrackerObjectQuad<T> tracker, int desiredWidth, int desiredHeight)
	{
		this.tracker = tracker;
		this.desiredWidth = desiredWidth;
		this.desiredHeight = desiredHeight;

		addMouseListener(this);
		addMouseMotionListener(this);

		window =        new JFrame("Object Tracking");
        cpanel =        new JFrame("Control Panel");
        
        stats =         new JPanel();
        buttons =       new JPanel();
        network =       new JPanel();
        
        // === Set Layout
        cpanel.getContentPane().setLayout(new FlowLayout());
        stats.setLayout(new GridLayout(0, 1));
        buttons.setLayout(new GridLayout(0, 1));
        network.setLayout(new GridLayout(0, 1));

        show =          new JButton("Show/Hide Guide");
        selectObject =  new JButton("Select Object Mode");
        selectTarget =  new JButton("Select Target Mode");
        removeTarget =  new JButton("Reset Target");
        Connect =       new JButton("Connect");
        sendCom =       new JButton("Send Command");
        
        status =        new JLabel("MODE: Object");
        realCoord =     new JLabel("Real: X: Y:");
        relaCoord =     new JLabel("Relative: X: Y:");
        targetCoord =   new JLabel("Target: X: Y:");
        receive =       new JLabel("Receive Messages");
        
        conStatus =     new JLabel("Not Connected");
        portL =         new JLabel("Port:");
        ipL =           new JLabel("IP Address:");
        
        portF =         new JTextField("1112");
        ipF =           new JTextField("10.0.0.8");
        
        status.setPreferredSize(new Dimension(150, 30));
        show.setPreferredSize(new Dimension(150, 30));
        portL.setPreferredSize(new Dimension(150, 30));

        
        // === Define Action Listeners
        show.addActionListener(new ActionListener(){  
            public void actionPerformed(ActionEvent e){  
                    if (shown == true){
                        shown = false;
                    }else{
                        shown = true;
                    }
            }  
        });

        selectObject.addActionListener(new ActionListener(){  
            public void actionPerformed(ActionEvent e){  
                    modeSwitch = 0;
                    selectTarget.setEnabled(true);
                    selectObject.setEnabled(false);
            }  
        });

        selectTarget.addActionListener(new ActionListener(){  
            public void actionPerformed(ActionEvent e){  
                    modeSwitch = 1;
                    selectTarget.setEnabled(false);
                    selectObject.setEnabled(true);
            }  
        });
        
        removeTarget.addActionListener(new ActionListener(){  
            public void actionPerformed(ActionEvent e){  
                    tar = false;
            }  
        });    

        Connect.addActionListener(new ActionListener(){  
            public void actionPerformed(ActionEvent e){  
                    connectPort();
            }  
        });
        
        sendCom.addActionListener(new ActionListener(){  
            public void actionPerformed(ActionEvent e){  
                //Send information to the EV3
                if (connected){
                    //Extra precaution
                    try {
                        //Send coordinates
                        send = getXY();
                        //send = {target1.getX(), target1.gety()};
                        out.writeUTF(String.valueOf(send[0]));
                        out.writeUTF(String.valueOf(send[1]));
                        send = getTargetXY();
                        out.writeUTF(String.valueOf(send[0]));
                        out.writeUTF(String.valueOf(send[1]));
                        
                    }catch(IOException ioe){
                        conStatus.setText("Error sending command");
                        System.out.println(ioe);
                    }
                }
                
            }  
        });

        //?Set content pane
		window.setContentPane(this);
		window.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
	}

	/**
	 * Invoke to start the main processing loop.
	 */
	public void process() {
		Webcam webcam = UtilWebcamCapture.openDefault(desiredWidth, desiredHeight);

		// adjust the window size and let the GUI know it has changed
		Dimension actualSize = webcam.getViewSize();
		setPreferredSize(actualSize);
		setMinimumSize(actualSize);
		window.setMinimumSize(actualSize);
		window.setPreferredSize(actualSize);
		window.setVisible(true);
        
        // = Add to Control Panel
        cpanel.add(stats);
        cpanel.add(buttons);
        cpanel.add(network);
        
        // = Add to Stats sub panel
        stats.add(status);
        stats.add(realCoord);
        stats.add(relaCoord);
        stats.add(targetCoord);
        
        // = Add to Buttons sub panel
        buttons.add(show);
        buttons.add(selectObject);
        buttons.add(selectTarget);
        buttons.add(removeTarget);
        buttons.add(sendCom);
        
        // = Add to Network sub panel
        network.add(conStatus);
        network.add(portL);
        network.add(portF);
        network.add(ipL);
        network.add(ipF);
        network.add(Connect);
        
        selectTarget.setEnabled(true);
        selectObject.setEnabled(false);
        sendCom.setEnabled(false);
        
        // === Render Frame
        cpanel.pack(); 
        cpanel.setVisible(true);

		// create
		T input = tracker.getImageType().createImage(actualSize.width,actualSize.height);

		workImage = new BufferedImage(input.getWidth(),input.getHeight(),BufferedImage.TYPE_INT_RGB);

		while( true ) {
			BufferedImage buffered = webcam.getImage();
			ConvertBufferedImage.convertFrom(webcam.getImage(), input, true);
            
            try{
                if (connected && in.available() != 0){
                    response = in.readUTF();
                    receive.setText(response);
                    if (response.equals("check")){
                        send = getXY();
                        out.writeUTF(String.valueOf(send[0]));
                        out.writeUTF(String.valueOf(send[1]));
                    }else{
                        
                    }
                }
                }catch(IOException ioe) {
            }
            
			// mode is read/written to by the GUI also
			int mode = this.mode;

			boolean success = false;
			if( mode == 2 ) {
				RectangleCorner2D_F64 rect = new RectangleCorner2D_F64();
				rect.set(point0.x, point0.y, point1.x, point1.y);
				UtilPolygons2D_F64.convert(rect, target);
				success = tracker.initialize(input,target);
				this.mode = success ? 3 : 0;
			} else if( mode == 3 ) {
				success = tracker.process(input,target);
			}

			synchronized( workImage ) {
				// copy the latest image into the work buffered
				Graphics2D g2 = workImage.createGraphics();
                
                getXY();
                
				g2.drawImage(buffered,0,0,null);
                
                if(tar){
                    drawTarget(g2);
                }
                
                if(shown){drawOrigin(g2);}
				// visualize the current results
				if (mode == 1) {
					drawSelected(g2);
				} else if (mode == 3) {
					if( success ) {
						drawTrack(g2);
					}
				}
                
                if (mode == 3 && tar && connected){
                    sendCom.setEnabled(true);
                }else{
                    sendCom.setEnabled(false);
                }
                
                
                    
			}

			repaint();
		}
	}
    
    public void connectPort(){
        try {
            ip = ipF.getText();
            port = Integer.parseInt(portF.getText());
            sock = new Socket(ip, port);
            out = new DataOutputStream(sock.getOutputStream());
            in = new DataInputStream(sock.getInputStream());
            
            conStatus.setText("Connected");
            connected = true;
        }catch(IOException ioe){
            conStatus.setText("Connection Failed");
        }
    }

	@Override
	public void paint (Graphics g) {
		if( workImage != null ) {
			// draw the work image and be careful to make sure it isn't being manipulated at the same time
			synchronized (workImage) {
				g.drawImage(workImage, 0, 0, null);
			}
		}
	}

	private void drawSelected( Graphics2D g2 ) {
		g2.setColor(Color.RED);
		g2.setStroke( new BasicStroke(3));
		g2.drawLine(point0.getX(),point0.getY(),point1.getX(),point0.getY());
		g2.drawLine(point1.getX(),point0.getY(),point1.getX(),point1.getY());
		g2.drawLine(point1.getX(),point1.getY(),point0.getX(),point1.getY());
		g2.drawLine(point0.getX(),point1.getY(),point0.getX(),point0.getY());
        
	}
    
    private void drawOrigin( Graphics2D g2 ) {
		g2.setColor(Color.MAGENTA);
		g2.setStroke( new BasicStroke(3));
        g2.draw(new Arc2D.Double(0, 100, 640, 640, 0, 180, Arc2D.PIE));
        g2.setColor(Color.YELLOW);
		g2.drawLine(origin_x-10,origin_y+10,origin_x+10,origin_y+10);
        g2.drawLine(origin_x-10,origin_y-10,origin_x+10,origin_y-10);
        g2.drawLine(origin_x-10,origin_y-10,origin_x-10,origin_y+10);
        g2.drawLine(origin_x+10,origin_y-10,origin_x+10,origin_y+10);
	}

	private void drawTrack( Graphics2D g2 ) {
		g2.setStroke(new BasicStroke(3));
		g2.setColor(Color.RED);
		g2.drawLine((int)target.a.getX(),(int)target.a.getY(),(int)target.b.getX(),(int)target.b.getY());
		g2.setColor(Color.BLUE);
		g2.drawLine((int)target.b.getX(),(int)target.b.getY(),(int)target.c.getX(),(int)target.c.getY());
		g2.setColor(Color.GREEN);
		g2.drawLine((int)target.c.getX(),(int)target.c.getY(),(int)target.d.getX(),(int)target.d.getY());
		g2.setColor(Color.DARK_GRAY);
		g2.drawLine((int)target.d.getX(),(int)target.d.getY(),(int)target.a.getX(),(int)target.a.getY());
	}
    
    private double[] getXY(){
        // === Calculating X/Y
        
        x_calc = (target.a.getX() + target.b.getX())/2;
        y_calc = (target.a.getY() + target.d.getY())/2;
        
        double[] res = {x_calc, y_calc};
        
        realCoord.setText("Real: X:" + String.valueOf((int)x_calc) + " Y:" + String.valueOf((int)y_calc));
        
        x_calc = (x_calc - origin_x)/pixel_to_cm;
        y_calc = (origin_y - y_calc)/pixel_to_cm;
        
        String x_str = df.format(x_calc);
        String y_str = df.format(y_calc);
        
        relaCoord.setText("Relative: X:" + x_str + " Y:" + y_str);
        
        return res;
    }
    
    private double[] getTargetXY(){
        // === Calculating X/Y
        double xT = (target1.getX() - origin_x)/pixel_to_cm;
        double yT = (origin_y - target1.getY())/pixel_to_cm;
        
        String xTS = df.format(xT);
        String yTS = df.format(yT);
        
        targetCoord.setText("Target: X:" + xTS + " Y:" + yTS);
        
        double[] res = {target1.getX(), target1.getY()};
        
        return res;
    }

	private void drawTarget( Graphics2D g2 ) {
		g2.setColor(Color.RED);
		g2.setStroke( new BasicStroke(5));
		g2.drawOval(target1.getX(), target1.getY(), 10, 10);
	}

    // === Mouse Event Listener
    
	@Override
	public void mousePressed(MouseEvent e) {
        if (modeSwitch==0){
            point0.set(e.getX(),e.getY());
            point1.set(e.getX(),e.getY());
            mode = 1;
        }else{
            
        }
	}

	@Override
	public void mouseReleased(MouseEvent e) {
        if (modeSwitch==0){ 
            point1.set(e.getX(),e.getY());
            mode = 2;
        }else{
            target1.set(e.getX(),e.getY());
            tar = true;
            getTargetXY();
        }
	}

	@Override public void mouseClicked(MouseEvent e) {
        if (modeSwitch==0){ mode = 0; }
    }

	@Override public void mouseEntered(MouseEvent e) {}

	@Override public void mouseExited(MouseEvent e) {}

	@Override public void mouseDragged(MouseEvent e) {
        if (modeSwitch==0){
            if( mode == 1 ) {
                point1.set(e.getX(),e.getY());
            }
        }
	}

	@Override
	public void mouseMoved(MouseEvent e) {}

	public static void main(String[] args) {

		ImageType<MultiSpectral<ImageUInt8>> colorType = ImageType.ms(3, ImageUInt8.class);

		TrackerObjectQuad tracker =
				FactoryTrackerObjectQuad.circulant(null, ImageUInt8.class);
//				FactoryTrackerObjectQuad.sparseFlow(null,ImageUInt8.class,null);
//				FactoryTrackerObjectQuad.tld(null,ImageUInt8.class);
//				FactoryTrackerObjectQuad.meanShiftComaniciu2003(new ConfigComaniciu2003(), colorType);
//				FactoryTrackerObjectQuad.meanShiftComaniciu2003(new ConfigComaniciu2003(true),colorType);
//				FactoryTrackerObjectQuad.meanShiftLikelihood(30,5,255, MeanShiftLikelihoodType.HISTOGRAM,colorType);


		CAM_Tracker app = new CAM_Tracker(tracker,660,520);

		app.process();
        
        Runtime.getRuntime().addShutdownHook(new Thread(){public void run(){
            try {
                sock.close();
                System.out.println("The server is shut down!");
            } catch (IOException e) { /* failed */ }
        }});
	}
}

