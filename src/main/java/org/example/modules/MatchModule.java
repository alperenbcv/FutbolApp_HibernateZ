package org.example.modules;



import org.example.entities.GameDate;
import org.example.entities.Match;
import org.example.entities.TeamStats;
import org.example.enums.EMatchStatus;
import org.example.models.DatabaseModels;
import org.example.models.MatchModel;
import org.example.utility.MatchEngine;

import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.stream.Collectors;

public class MatchModule {
	static Scanner sc = new Scanner(System.in);
	static LocalDate currentDate;
	static MatchEngine matchEngine = new MatchEngine();
	static GameDate gameDate;

	static {
		Optional<GameDate> byId = DatabaseModels.gameDateController.findById(1);
		if (byId.isPresent()) {
			gameDate = byId.get();
		}
		currentDate = gameDate.getGameDate();
	}

	public static void saveDatesToFile() {
		gameDate.setGameDate(currentDate);
		DatabaseModels.gameDateController.updateGameDate(gameDate);
	}

	public static void unsavedDate(){

		gameDate = new GameDate(LocalDate.of(2024,8,23));
	}

	public static void readDate() {

		Optional<GameDate> byGameDate = DatabaseModels.gameDateController.findById(1);
		byGameDate.ifPresent(gameDate -> currentDate = gameDate.getGameDate());

	}



	public static void startMatchMenu() {
		int userInput;
		do {
			userInput = matchMenuEntry();
			matchMenuSelection(userInput);
		} while (userInput != 0);
	}

	private static int matchMenuEntry() {
		int userInput=-1;
		boolean validInput = false;

		while(!validInput) {
			System.out.println("\n---------Match Menu--------------");
			System.out.println("1-Matches of the Day");
			System.out.println("2-Display Date");
			System.out.println("3-Display Results by Date");
			System.out.println("4-Display Yesterday Results");
			System.out.println("5-Skip Day");
			System.out.println("0-FootballApp.Main Menu");
			System.out.print("Selection: ");
			try {
				userInput = sc.nextInt();
				sc.nextLine();
				if(userInput >= 0 && userInput <= 5) {
					validInput = true;
				}
				else {
					System.out.println("\nPlease enter a valid option!");
				}
			} catch (InputMismatchException e) {
				System.out.println("\nPlease enter a numeric value!");
				sc.next();
			}
		}
		return userInput;
	}

	private static void matchMenuSelection(int userInput) {
		switch (userInput) {
			case 1 -> displayTodaysGame();
			case 2 -> displayCurrentDate();
			case 3 -> displayResults();
			case 4 -> displayYesterdaysResults();
			case 5 -> skipDay();
			case 0 -> System.out.println("\nReturning to FootballApp.Main Menu...\n");
			default-> System.out.println("Please enter a valid value!");
		}
	}

	private static void displayYesterdaysResults() {
		List<Match> matchesOfTheDay =
				DatabaseModels.matchController.findAll().stream()
						.filter(match -> match.getMatchDate().equals(currentDate.minusDays(1)))
						.collect(Collectors.toList());

		if(matchesOfTheDay.isEmpty()){
			System.out.println("\nNo match played yesterday! " + currentDate.minusDays(1));
			return;
		}
		for (Match match : matchesOfTheDay) {
			MatchModel mm = new MatchModel(DatabaseModels.getInstance(), match);
			mm.displayPlayedMatchInfo();
		}
	}

	private static void displayResults() {

		LocalDate selectedDate = null;
		boolean validInput = false;

		while (!validInput) {
			System.out.println("\nEnter a date to display match results (YYYY-MM-DD): ");
			try {
				selectedDate = LocalDate.parse(sc.nextLine());
				validInput = true;
			} catch (DateTimeParseException e) {
				System.out.println("Please enter a valid date (YYYY-MM-DD)!");
			}
		}

		final LocalDate finalSelectedDate = selectedDate;

		List<Match> matchesOfTheDay =
				DatabaseModels.matchController.findAll().stream()
						.filter(match -> match.getMatchDate().equals(finalSelectedDate))
						.collect(Collectors.toList());

		if (matchesOfTheDay.isEmpty()) {
			System.out.println("\nNo matches played for the day!");
		} else {
			for (Match match : matchesOfTheDay) {
				MatchModel mm = new MatchModel(DatabaseModels.getInstance(), match);
				mm.displayPlayedMatchInfo();
			}
		}
	}



	private static void skipDay() {
		Integer i = simulateGames();
		System.out.println("\n"+i + " games simulated" );
		updateTeamStats();
		currentDate = currentDate.plusDays(1);
		saveDatesToFile();
		System.out.println("Day skipped to: " + currentDate);

	}

	public static Integer simulateGames() {
		List<Match> matchesOfTheDay =
				DatabaseModels.matchController.findAll().stream().filter(match -> match.getMatchDate().equals(currentDate))
						.filter(match -> match.getStatus() != EMatchStatus.PLAYED)
						.toList();

		if (matchesOfTheDay.isEmpty()) {
			System.out.println("\nNo matches scheduled for today!");
		}
		else {

			for(Match match:matchesOfTheDay){
				if(match.getHomeTeam().getId() == DatabaseModels.teamController.findByFieldNameAndValue("teamName","BYE").getFirst().getId()){
					match.setHomeTeamScore(0);
					match.setAwayTeamScore(3);
					match.setStatus(EMatchStatus.PLAYED);
				}
				else if(match.getAwayTeam().getId() == DatabaseModels.teamController.findByFieldNameAndValue("teamName","BYE").getFirst().getId()){
					match.setHomeTeamScore(3);
					match.setAwayTeamScore(0);
					match.setStatus(EMatchStatus.PLAYED);
				}
				else {
					matchEngine.simulateMatch(match);
					match.setStatus(EMatchStatus.PLAYED);
				}
				DatabaseModels.matchController.update(match);
			}
		}
		int size = matchesOfTheDay.size();
		return size;
	}


	private static void updateTeamStats() {

		List<Match> allPlayedMatches = DatabaseModels.matchController.findAll().stream()
				.filter(match -> match.getStatus() == EMatchStatus.PLAYED)
				.filter(match -> match.getMatchDate().equals(currentDate))
				.toList();


		for (Match match : allPlayedMatches) {


			TeamStats homeTeamStats = DatabaseModels.teamStatController
					.findByTeamId(match.getHomeTeam()).get();


			TeamStats awayTeamStats = DatabaseModels.teamStatController
					.findByTeamId(match.getAwayTeam()).get();

			if (homeTeamStats != null) {
				homeTeamStats.setGoalScored(homeTeamStats.getGoalScored() + match.getHomeTeamScore());
				homeTeamStats.setGamesPlayed(homeTeamStats.getGamesPlayed() + 1);
				homeTeamStats.setAverage(homeTeamStats.getAverage() + (match.getHomeTeamScore() - match.getAwayTeamScore()));
				homeTeamStats.setGoalConceded(homeTeamStats.getGoalConceded() + match.getAwayTeamScore());

				if (match.getHomeTeamScore() > match.getAwayTeamScore()) {
					homeTeamStats.setGamesWon(homeTeamStats.getGamesWon() + 1);
					homeTeamStats.setTotalPoint(homeTeamStats.getTotalPoint() + 3);
				} else if (match.getHomeTeamScore() < match.getAwayTeamScore()) {
					homeTeamStats.setGamesLost(homeTeamStats.getGamesLost() + 1);
				} else {
					homeTeamStats.setGamesDrawn(homeTeamStats.getGamesDrawn() + 1);
					homeTeamStats.setTotalPoint(homeTeamStats.getTotalPoint() + 1);
				}
				homeTeamStats.setLastUpdateDate(LocalDate.now());
				DatabaseModels.teamStatController.update(homeTeamStats);
			}

			if (awayTeamStats != null) {
				awayTeamStats.setGoalScored(awayTeamStats.getGoalScored() + match.getAwayTeamScore());
				awayTeamStats.setGamesPlayed(awayTeamStats.getGamesPlayed() + 1);
				awayTeamStats.setAverage(awayTeamStats.getAverage() + (match.getAwayTeamScore() - match.getHomeTeamScore()));
				awayTeamStats.setGoalConceded(awayTeamStats.getGoalConceded() + match.getHomeTeamScore());

				if (match.getAwayTeamScore() > match.getHomeTeamScore()) {
					awayTeamStats.setGamesWon(awayTeamStats.getGamesWon() + 1);
					awayTeamStats.setTotalPoint(awayTeamStats.getTotalPoint() + 3);
				} else if (match.getAwayTeamScore() < match.getHomeTeamScore()) {
					awayTeamStats.setGamesLost(awayTeamStats.getGamesLost() + 1);
				} else {
					awayTeamStats.setGamesDrawn(awayTeamStats.getGamesDrawn() + 1);
					awayTeamStats.setTotalPoint(awayTeamStats.getTotalPoint() + 1);
				}
				awayTeamStats.setLastUpdateDate(LocalDate.now());
				DatabaseModels.teamStatController.update(awayTeamStats);
			}
		}
	}
	private static void displayCurrentDate() {
		System.out.println("\nCurrent Game Date: " + currentDate);
	}

	private static void displayTodaysGame() {
		System.out.println("\nMatches of the Day (" + currentDate + "):");

		List<Match> matchesOfTheDay =
				DatabaseModels.matchController.findAll().stream().filter(match -> match.getMatchDate().equals(currentDate))
						.collect(Collectors.toList());

		System.out.println("\n"+matchesOfTheDay.size() + " matches scheduled today!");
		System.out.println();

		if(matchesOfTheDay.isEmpty()) {
			System.out.println("No matches scheduled for today!");
		}
		else {
			for (Match match:matchesOfTheDay){
				MatchModel mm = new MatchModel(DatabaseModels.getInstance(), match);
				mm.displayScheduledMatchInfo();
			}

		}
	}
}