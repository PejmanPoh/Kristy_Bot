import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A simple multi-threaded task engine
 * @author XspeedPL
 */
public final class Scheduler extends Thread implements AutoCloseable
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
					if (t.tick()) exs.execute(t);
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
	 * @param t The task to execute
	 * @return An unique task ID
	 */
	public final long addTask(final Task t)
	{
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
	
	public static abstract class Task implements Runnable
	{
		/** The unique task ID */
		public final long ID;
		
		private boolean started, completed;
		private int timeleft;
		
		/**
	     * @param time Delay in 500ms time units
		 */
		public Task(final int time)
		{
			ID = MyBot.rand.nextLong();
			timeleft = time;
		}
		
		/**
		 * Internal use only
		 */
		@Override
		public void run()
		{
			main();
			if (timeleft < 1) completed = true;
		}
		
		/**
		 * Scheduled tasks's code
		 */
		public abstract void main();
		
		/**
		 * Instruct a task to run again
		 * @param time Delay in 500ms time units
		 */
		public final void reschedule(final int time)
		{
			timeleft = time;
			started = false;
		}
		
		/**
		 * Takes care of the countdown to the start of the task
		 * @return True if the task should be started, false otherwise
		 */
		private final boolean tick()
		{
			if (timeleft > 1) --timeleft;
			else if (!started)
			{
				started = true;
				return true;
			}
			return false;
		}
		
		/**
		 * Checks whenever this task has finished and hasn't been rescheduled
		 * @return True if the task is going to be removed, false otherwise
		 */
		public final boolean isCompleted() { return completed; }
	}
}
