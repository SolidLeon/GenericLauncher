package launcher.gui;

import java.awt.Color;
import java.io.OutputStream;

/**
 * SolidLeon #4 20150227 
 * 
 * Status/Progress update listener
 * @author SolidLeon
 *
 */
public interface IStatusListener {
	void setCurrentProgress(int value, int min, int max, String text);
	void setCurrentProgress(int value);
	void setCurrentProgressToMax();
	void setCurrentProgress(String text);
	int getCurrentProgress();
	
	void setOverallProgress(int value, int min, int max);
	void setOverallProgress(int value);
	void setOverallProgress(String text);
	int getOverallProgress();
	void addOverallProgress(int i);
//	OutputStream getOutputStream()
	public void appendText(Color fg, Color bg, String str);
	void setStatusCompletedExecCommandOnExit(Runnable runner);
	
}
