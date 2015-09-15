
public final class TimertestTask extends Scheduler.Task
{
	final int ticks;
	final long start;
	long end;
	
	public TimertestTask(final int time)
	{
		super("timertest", time);
		ticks = time;
		start = System.nanoTime();
		end = -1;
	}

	@Override
	public final void main()
	{
		end = System.nanoTime();
	}
	
	/**
	 * Get the test result
	 * @return Measured amount of milliseconds per tick
	 */
	public final float getResult()
	{
		return end == -1 ? -1 : Config.round((end - start) / ticks / 1000000, 4);
	}
}
