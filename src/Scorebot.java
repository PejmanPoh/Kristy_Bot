import java.util.ArrayList;
import java.util.Map;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter.Listener;

/**
 * The Scorebot ported from HLTV JavaScript (WORK IN PROGRESS)
 * 
 * @author XspeedPL
 */
final class Scorebot
{
	int logLength = 1500;
	final ArrayList<Player> players = new ArrayList<Player>();

	int matchRoundtime = 105;
	int matchBombtime = 35;

	public static final boolean inArray(final String[] arr, final String val, final boolean caseSensitive)
	{
		for (int i = 0; i < arr.length; i++)
		{
			if (caseSensitive)
			{
				if (arr[i].toLowerCase() == val.toLowerCase())
					return true;
			}
			else if (arr[i] == val)
				return true;
		}
		return false;
	}

	final class Player
	{
		public Player()
		{

		}

		int id;
		String side;
		String name;
		int kills;
		int deaths;
		boolean alive;
		int assists;
	}

	boolean reconnected = false;

	Socket socket;

	public final void stopBot()
	{
		socket.disconnect();
	}

	public final boolean getBombState(Object log, boolean bombState)
	{
		boolean foo = bombState;

		iterEvents(log, (event, data) ->
		{
			switch (event)
			{
				case "RoundStart":
					foo = false;
					break;
				case "RoundEnd":
					foo = false;
					break;

				case "BombPlanted":
					foo = true;
					break;
				case "BombDefused":
					foo = false;
					break;

				default:

			}
		});
		return foo;
	}

	public final void iterEvents(Object log, Object eventHandler)
	{
		final String json = JSON.parse(log);
		$q.each(json.log, (key, value) ->
		{
			$q.each(value, eventHandler);
		});
	}

	public final boolean hasNewRound(Object log)
	{
		boolean ret = false;
		iterEvents(log, (event, data) ->
		{
			switch (event)
			{
				case "RoundStart":
					ret = true;
					break;
			}
		});
		return ret;
	}

	public final Object getMap(Object log)
	{
		Object ret = null;

		iterEvents(log, (event, data) ->
		{
			switch (event)
			{
				case "MatchStarted":
					if (ret == null)
					{
						ret = data.map;
					}
					break;
			}
		});
		return ret;
	}

	public final String generateFormattedLog(Object log)
	{
		String formatted = "";

		iterEvents(log, (event, data) ->
		{
			switch (event)
			{
				case "RoundStart":
					formatted += "<span>Round started</span><br/><br/>";
					break;
				case "RoundEnd":
					formatted += "<span>Round over - Winner: " + formatSide(data.winner == "TERRORIST" ? "T" : "CT", data.winner) + " (" + formatSide(data.terroristScore, "TERRORIST") + "-" + formatSide(data.counterTerroristScore, "CT") + ")<br/>";
					break;
				case "Kill":
					killer = formatSide(data.killerName, data.killerSide);
					victim = formatSide(data.victimName, data.victimSide);
					hsString = data.headShot ? " (headshot)" : "";
					formatted += killer + " killed " + victim + " with " + data.weapon + hsString + "<br/>";
					break;
				case "BombPlanted":
					formatted += formatSide(data.playerName, "TERRORIST") + " planted the bomb<br/>";
					break;
				case "BombDefused":
					formatted += formatSide(data.playerName, "CT") + " defused the bomb<br/>";
					break;
				case "PlayerJoin":
					formatted += data.playerName + " joined the game<br/>";
					break;
				case "PlayerQuit":
					formatted += formatSide(data.playerName, data.playerSide) + " quit the game<br/>";
					break;
				case "Suicide":
					formatted += formatSide(data.playerName, data.side) + " committed suicide<br/>";

				default:
					// formatted += "missing event: " + event + "<br/>"

			}
		});
		return formatted;
	}

	public final void startBot()
	{
		final int port = 10022;

		socket = IO.socket("http://scorebot2.hltv.org:" + port);

		socket.on("connect", new Listener()
		{
			@Override
			public final void call(final Object... args)
			{
				if (!reconnected)
				{
					socket.on("log", new Listener()
					{
						@Override
						public final void call(final Object... args)
						{
							String log = (String)args[0];
							console.log(log);
							Object plantedInLog = getBombState(log, bombPlanted);
	
							Object formattedLog = generateFormattedLog(log);
							if (hasNewRound(log))
							{
								roundTime = matchRoundtime;
							}
	
							if (!bombPlanted && plantedInLog && !hasNewRound(log))
							{
								bombPlanted = true;
								roundTime = matchBombtime;
							}
							if (bombPlanted && !plantedInLog)
							{
								bombPlanted = false;
								if (!hasNewRound(log))
								{
									roundTime = 0;
								}
							}
	
							Object map = getMap(log);
							if (map != null)
							{
								$q("#map").html(map);
							}
	
							$q("#gamelog").html(formattedLog + $q("#gamelog").html());
						}
					});

					socket.on("score", new Listener()
					{
						@Override
						public final void call(final Object... args)
						{
							final Map<String, String> score = (Map<String, String>)args[0];
							console.log(score);
							document.getElementById("ctscore").innerHTML = score["ctScore"];
							document.getElementById("tscore").innerHTML = score["tScore"];
						}
					});

					socket.on("scoreboard", new Listener()
					{
						@Override
						public final void call(final Object... args)
						{
							final Object scoreboard = args[0];
							players.clear();
							console.log(scoreboard);
							for (int i = 0; i < scoreboard["CT"].length; ++i)
							{
								Player player = scoreboard["CT"][i];
								players.push(new Player(player.id, "CT", player.name, player.score, player.deaths, player.alive, player.assists));
							}
							for (int i = 0; i < scoreboard["TERRORIST"].length; ++i)
							{
								Player player = scoreboard["TERRORIST"][i];
								players.push(new Player(player.id, "TERRORIST", player.name, player.score, player.deaths, player.alive, player.assists));
							}
						}
					});
					socket.emit("readyForMatch", listid);
				}
			}
		});

		socket.on("reconnect", new Listener()
		{
			@Override
			public final void call(final Object... args)
			{
				reconnected = true;
				socket.emit("readyForMatch", listid);
			}
		});
	}
}