import java.io.Serializable;

public class CommClientWrapper implements Serializable{

	private static final long serialVersionUID = 1L;
	public String name;
	public Point point;
	public Direction orientation;
	
	public CommClientWrapper(String s, Point p, Direction o)
	{
		this.name = s;
		this.point = p;
		this.orientation = o;
	}
	
	public CommClientWrapper(String s)
	{
		this.name = s;
		this.point = null;
		this.orientation = null;
	}
	
	public CommClientWrapper(CommClientWrapper cw)
	{
		this.name = new String(cw.name);
		this.point = new Point(cw.point);
		this.orientation = cw.orientation;
	}
}
