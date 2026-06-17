package com.gantaro.mysterybot.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.Map;

public class LineBotAutoConfigurationEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

	private static final String LINE_BOT_CHANNEL_SECRET = "line.bot.channel-secret";

	private static final String SPRING_AUTOCONFIGURE_EXCLUDE = "spring.autoconfigure.exclude";

	private static final String LINE_BOT_AUTOCONFIGURATION_EXCLUDES = String.join(",",
			"com.linecorp.bot.spring.boot.webmvc.configuration.LineBotWebMvcConfigurer",
			"com.linecorp.bot.spring.boot.handler.configuration.LineBotHandlerConfiguration");

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		if (!isBlank(environment.getProperty(LINE_BOT_CHANNEL_SECRET))
				|| !isBlank(environment.getProperty(SPRING_AUTOCONFIGURE_EXCLUDE))) {
			return;
		}

		environment.getPropertySources().addLast(new MapPropertySource(
				"lineBotDisabledDefaults",
				Map.of(SPRING_AUTOCONFIGURE_EXCLUDE, LINE_BOT_AUTOCONFIGURATION_EXCLUDES)));
	}

	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE;
	}

	private static boolean isBlank(String value) {
		return value == null || value.isBlank();
	}

}
