import java.util.TimerTask;

public class TimeOutHandler extends TimerTask {

	
	Router master;
	
	public TimeOutHandler(Router master){
		this.master = master;
	
	}
	
	
	public void run(){
		master.processTimeout();
		
	}
	
	
}
