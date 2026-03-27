package com.hackathon.finance;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BackendApplication {

	private static final String DEFAULT_POSTGRES_DB_URL = "jdbc:postgresql://localhost:5432/finance_tracker_pg";
	private static final String DEFAULT_DB_USERNAME = "postgres";
	private static final String DEFAULT_DB_PASSWORD = "1229";

	public static void main(String[] args) {
		ensurePostgresDatabaseExists();
		SpringApplication.run(BackendApplication.class, args);
	}

	private static void ensurePostgresDatabaseExists() {
		String dbUrl = env("DB_URL", DEFAULT_POSTGRES_DB_URL);
		if (!dbUrl.startsWith("jdbc:postgresql://")) {
			return;
		}

		String username = env("DB_USERNAME", DEFAULT_DB_USERNAME);
		String password = env("DB_PASSWORD", DEFAULT_DB_PASSWORD);
		String adminUrl = env("DB_ADMIN_URL", buildAdminUrl(dbUrl));
		String databaseName = extractDatabaseName(dbUrl);

		try (Connection connection = DriverManager.getConnection(adminUrl, username, password)) {
			if (databaseExists(connection, databaseName)) {
				return;
			}
			try (Statement statement = connection.createStatement()) {
				statement.execute("CREATE DATABASE \"" + databaseName.replace("\"", "\"\"") + "\"");
			}
		} catch (SQLException ex) {
			throw new IllegalStateException("Failed to create PostgreSQL database '" + databaseName + "'", ex);
		}
	}

	private static boolean databaseExists(Connection connection, String databaseName) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement(
				"select 1 from pg_database where datname = ?")) {
			statement.setString(1, databaseName);
			try (ResultSet resultSet = statement.executeQuery()) {
				return resultSet.next();
			}
		}
	}

	private static String buildAdminUrl(String dbUrl) {
		try {
			URI uri = new URI(dbUrl.substring("jdbc:".length()));
			String query = uri.getQuery();
			StringBuilder builder = new StringBuilder("jdbc:postgresql://")
					.append(uri.getHost());
			if (uri.getPort() != -1) {
				builder.append(':').append(uri.getPort());
			}
			builder.append("/postgres");
			if (query != null && !query.isBlank()) {
				builder.append('?').append(query);
			}
			return builder.toString();
		} catch (URISyntaxException ex) {
			throw new IllegalStateException("Invalid PostgreSQL DB_URL: " + dbUrl, ex);
		}
	}

	private static String extractDatabaseName(String dbUrl) {
		try {
			URI uri = new URI(dbUrl.substring("jdbc:".length()));
			String path = uri.getPath();
			if (path == null || path.length() <= 1) {
				throw new IllegalStateException("Database name is missing in DB_URL: " + dbUrl);
			}
			return path.substring(1);
		} catch (URISyntaxException ex) {
			throw new IllegalStateException("Invalid PostgreSQL DB_URL: " + dbUrl, ex);
		}
	}

	private static String env(String key, String fallback) {
		String value = System.getenv(key);
		return value == null || value.isBlank() ? fallback : value;
	}

}
