import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A simple multi-threaded task engine
 * @author Xspeed
 */
final class Scheduler extends Thread implements AutoCloseable
{
	private final LinkedList<Task> tasks;
	private final ExecutorService exs;
	
	Scheduler()
	{
		tasks = new LinkedList<Task>();
		exs = Executors.newCachedThreadPool();
		setName("Task Scheduler");
		start();
	}
	
	@Override
	public final void run()
	{
		final LinkedList<Task> completed = new LinkedList<Task>();
		while (!exs.isShutdown())
		{
			try
			{
				Thread.sleep(500);
				for (final Task t : tasks)
				{
					t.tick();
					if (t.isCompleted()) completed.add(t);
				}
				for (final Task t : completed) tasks.remove(t);
				completed.clear();
			}
			catch (InterruptedException ex) { }
		}
		tasks.clear();
	}
	
	/**
	 * Add a task to the queue
	 * @param r The task to execute
	 * @param time Delay in 500ms time units
	 * @return An unique task ID
	 */
	public final long addTask(final Runnable r, final int time)
	{
		final Task t = new Task(r, time);
		tasks.add(t);
		return t.ID;
	}
	
	/**
	 * Cancel a queued task
	 * @param id The ID of the task to be cancelled
	 */
	public final void cancelTask(final long id)
	{
		final Iterator<Task> li = tasks.iterator();
		while (li.hasNext())
		{
			final Task t = li.next();
			if (t.ID == id)
			{
				tasks.remove(t);
				return;
			}
		}
	}
	
	@Override
	public final void close() throws Exception
	{
		interrupt();
		exs.shutdown();
	}
	
	private final class Task implements Runnable
	{
		private final long ID;
		private final Runnable task;
		private boolean started, completed;
		private int timeleft;
		
		Task(final Runnable r, final int time)
		{
			ID = MyBot.rand.nextLong();
			timeleft = time;
			task = r;
		}
		
		@Override
		public final void run()
		{
			task.run();
			completed = true;
		}
		
		final void tick()
		{
			if (timeleft > 1) --timeleft;
			else if (!started)
			{
				started = true;
				exs.execute(this);
			}
		}
		
		final boolean isCompleted() { return completed; }
	}
}
