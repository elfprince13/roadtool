//
//  RoadDisplay.java
//
//  Copyright Thomas Dickerson, 2012.
//  This file is part of RoadTool.
//
//  RoadTool is free software: you can redistribute it and/or modify it
//  under the terms of the GNU Lesser General Public License as published
//  by the Free Software Foundation, either version 3 of the License, or
//  (at your option) any later version.
//
//  RoadTool is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU Lesser General Public License for more details.
//
//  You should have received a copy of the GNU Lesser General Public License
//  long with RoadTool.  If not, see <http://www.gnu.org/licenses/>.
//

import javax.media.opengl.GLCanvas;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.media.opengl.GL;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.glu.GLU;
import javax.media.opengl.glu.GLUquadric;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.Serializable;
import java.nio.FloatBuffer;
import debug.Debugger;

enum RenderMode implements Serializable{	RECT,	FLATPOL	}
public class RoadDisplay implements Runnable, GLEventListener, KeyListener{
	public static final RenderMode RECT = RenderMode.RECT;
	public static final RenderMode FLATPOL = RenderMode.FLATPOL;
	private static RenderMode RENDERMODE = null; 
	
	static boolean bQuit = false;
	static Thread displayT = null;
	private float rotateT = 0.0f;
	private float scale = 0.0f;
	private float lookX = 0.0f;
	private float lookY = 0.0f;
	private static final GLU glu = new GLU();
	RoadGraph.RenderInfo data;
	
	public RoadDisplay(RoadGraph.RenderInfo ri){
		data = ri;
	}

	public void run() {
		Frame frame = new Frame("RoadTool OpenGL Displayer");
		GLCanvas canvas = new GLCanvas();
		canvas.addGLEventListener(this);
		frame.add(canvas);
		frame.setSize(640, 480);
		
		frame.addWindowListener(new WindowAdapter() {
								public void windowClosing(WindowEvent e) {
								bQuit = true;
								}
								});
		frame.setVisible(true);
		canvas.requestFocus();
		while( !bQuit ) {
			canvas.display();
		}
		bQuit = false;
	}	
	
	public void display(GLAutoDrawable gLDrawable) {
		final GL gl = gLDrawable.getGL();
		
		gl.glViewport(0, 0, gLDrawable.getWidth(), gLDrawable.getHeight());
			
		gl.glMatrixMode (GL.GL_PROJECTION);
		gl.glLoadIdentity();
		
		gl.glOrtho(data.minx - scale, data.maxx + scale, data.miny - scale, data.maxy + scale , data.minz - 4, data.maxz + 4);
	 
		gl.glMatrixMode (GL.GL_MODELVIEW);
		gl.glClear (GL.GL_COLOR_BUFFER_BIT);
		gl.glColor3f (0.0f, 1.0f, 1.0f);
		gl.glLoadIdentity (); 
		glu.gluLookAt (lookX, lookY, 1.0, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f);
		gl.glScalef (1.0f, 1.0f, 1.0f);
					
		gl.glClearColor(1.0f, 1.0f, 1.0f, 0.0f);
		gl.glClearDepth(1.0f);
		gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
								
		gl.glPushMatrix();
		
		//gl.glColor4f(0.0f, 0.0f, 0.0f, 1.0f);
		//GLUquadric quadric = glu.gluNewQuadric();
		//glu.gluSphere(quadric, 1.0f, 20, 20);
		
		gl.glColor4f(0.0f, 0.0f, 1.0f, 1.0f);
		gl.glLineWidth(1.0f);
		data.xyzA4.rewind();
		gl.glEnableClientState( GL.GL_VERTEX_ARRAY );
		gl.glVertexPointer(3, GL.GL_FLOAT, 0, data.xyzA4);
		gl.glDrawArrays(GL.GL_LINES, 0, data.catCounts[RoadEdge.A4] * 2);
		gl.glDisableClientState( GL.GL_VERTEX_ARRAY );
		
		gl.glColor4f(0.0f, 0.0f, 0.8f, 1.0f);
		gl.glLineWidth(2.0f);
		data.xyzA3.rewind();
		gl.glEnableClientState( GL.GL_VERTEX_ARRAY );
		gl.glVertexPointer(3, GL.GL_FLOAT, 0, data.xyzA3);
		gl.glDrawArrays(GL.GL_LINES, 0, data.catCounts[RoadEdge.A3] * 2);
		gl.glDisableClientState( GL.GL_VERTEX_ARRAY );
		
		gl.glColor4f(0.0f, 0.0f, 0.6f, 1.0f);
		gl.glLineWidth(3.0f);
		data.xyzA2.rewind();
		gl.glEnableClientState( GL.GL_VERTEX_ARRAY );
		gl.glVertexPointer(3, GL.GL_FLOAT, 0, data.xyzA2);
		gl.glDrawArrays(GL.GL_LINES, 0, data.catCounts[RoadEdge.A2] * 2);
		gl.glDisableClientState( GL.GL_VERTEX_ARRAY );
		
		gl.glColor4f(0.0f, 0.0f, 0.4f, 1.0f);
		gl.glLineWidth(4.0f);
		data.xyzA1.rewind();
		gl.glEnableClientState( GL.GL_VERTEX_ARRAY );
		gl.glVertexPointer(3, GL.GL_FLOAT, 0, data.xyzA1);
		gl.glDrawArrays(GL.GL_LINES, 0, data.catCounts[RoadEdge.A1] * 2);
		gl.glDisableClientState( GL.GL_VERTEX_ARRAY );
		
		if(data.xyzPOI != null){
			gl.glColor4f(1.0f, 0.0f, 0.75f, 1.0f);
			gl.glPointSize(3.0f);
			data.xyzPOI.rewind();
			gl.glEnableClientState( GL.GL_VERTEX_ARRAY );
			gl.glVertexPointer(3, GL.GL_FLOAT, 0, data.xyzPOI);
			gl.glDrawArrays(GL.GL_POINTS, 0, data.poiCount);
			gl.glDisableClientState( GL.GL_VERTEX_ARRAY );
		}												
		
		
	}
	
	public void displayChanged(GLAutoDrawable gLDrawable, 
							   boolean modeChanged, boolean deviceChanged) {
	}
	
	public void init(GLAutoDrawable gLDrawable) {
		final GL gl = gLDrawable.getGL();
		//gl.glShadeModel(GL.GL_SMOOTH);
		gl.glEnable(GL.GL_DEPTH_TEST);
		gl.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
		
		
		gl.glDepthFunc(GL.GL_LEQUAL);
		
		gl.glEnable (GL.GL_BLEND);
		gl.glBlendFunc (GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
		gl.glClearDepth(1.0f);
		
		scale = Math.max(Math.max(data.maxx - data.minx, data.maxy - data.miny), data.maxz - data.maxz) / 2.0f;
		//lookX = (data.minx + data.maxx) / 2.0f;
		//lookY = (data.miny + data.maxy) / 2.0f;
		Debugger.printInfo("scale: " + scale);
		
		gLDrawable.addKeyListener(this);
	}
	
	public void reshape(GLAutoDrawable gLDrawable, int x, 
						int y, int width, int height) {
		final GL gl = gLDrawable.getGL();
		if(height <= 0) {
			height = 1;
		}
	}
	
	public void keyPressed(KeyEvent e) {
		switch(e.getKeyCode()){
			case KeyEvent.VK_ESCAPE:
				RoadDisplay.bQuit = true;
				RoadDisplay.displayT = null;
				System.exit(0);
				break;
			case KeyEvent.VK_C:
				lookX = 0.0f;
				lookY = 0.0f;
				break;
			case KeyEvent.VK_UP:
				lookY += Math.sin(Math.PI / 900);
				break;
			case KeyEvent.VK_DOWN:
				lookY -= Math.sin(Math.PI / 900);
				break;
			case KeyEvent.VK_RIGHT:
				lookX += Math.cos(Math.PI / 7200);
				break;
			case KeyEvent.VK_LEFT:
				lookX -= Math.cos(Math.PI / 7200);
				break;
			case KeyEvent.VK_EQUALS:
				//Debugger.printInfo("zoom in");
				break;
			case KeyEvent.VK_MINUS:
				//Debugger.printInfo("zoom out");
				break;
			default:
				//Debugger.printInfo(""+e.getKeyCode());
		}
		//Debugger.printInfo("x: " + lookX + " Y: " + lookY);
	}
	
	public void keyReleased(KeyEvent e) {
	}
	
	public void keyTyped(KeyEvent e) {
	}
	
	public static void setRenderMode(RenderMode rm){
		if(RENDERMODE == null)	RENDERMODE = rm;
		else{
			Debugger.printError("setRenderMode has already been called once, may not be called again");
		}
	}
	
	public static RenderMode getRenderMode(){
		if(RENDERMODE != null)	return RENDERMODE;
		else{	throw new NullPointerException("must call RoadDisplay.setRenderMode() before RoadDisplay.getRenderMode()");	}
	}
	
	
}
