# STOCK_SENTINEL
This repository contains a  JAVA FX based project which monitors real-time stock price  the Dow Jones Industrial Average (DJIA) via the DIA ETF, built during the Forage Citi Technology Virtual Simulation

# Stock Sentinel

**Stock Sentinel** is a JavaFX application developed as part of the [Forage Citi Technology Virtual Simulation](https://www.theforage.com/virtual-internships). It provides a real-time dashboard for monitoring Dow Jones Industrial Average (DJIA) stock prices using the DIA ETF as a proxy, designed to help nontechnical Citi employees track market trends for risk management. The dashboard features a dynamic line chart, intuitive UI, and robust handling of live and historical data, built with UML design and Object-Oriented Programming (OOP) principles.

## Features
- **Real-Time Data**: Queries DIA stock prices every 15 seconds via the Alpha Vantage API (live during market hours, historical when closed, e.g., May 18, 2025, 11:32 AM EDT).
- **Line Chart**: Displays price trends in a pop-up window:
  - X-axis: Time (seconds since start).
  - Y-axis: Price (USD).
  - Updates with each query, showing all stored prices (up to 100 points).
- **Symbol Switching**: Allows users to change stock symbols (e.g., DIA to IBM) via a search box.
- **Market Closure Handling**: Shows historical 5-minute interval data when markets are closed, ensuring a dynamic chart.
- **Accessible UI**: Light-themed dashboard with tooltips, clear labels, and pastel-colored info boxes (symbol, currency, exchange, timezone) for nontechnical users.
- **Scalable Design**: Modular architecture supports future enhancements like hourly, daily, or monthly data views.
- **Error Handling**: Manages Alpha Vantage rate limits (5 calls/minute) with 60-second pauses on 429 errors.

## Why Stock Sentinel?
Stock Sentinel was built to make stock price monitoring accessible to nontechnical Citi employees. It applies classroom concepts like UML for system design and OOP for clean, modular code. As a Java novice, I leveraged transferable skills (conditionals, OOP from Python/C++) to learn Java quickly, reinforcing that programming is about concepts, not just languages. The project mirrors real-world challenges, such as API constraints and user-focused design, making it a practical simulation of Citi’s tech environment.

## Prerequisites
- **Java**: JDK 17 or later.
- **JavaFX**: Version 17 or later (included in some JDKs or as a separate module).
- **Gradle**: For dependency management (optional) (Maven can also be used but for this project Gradle was used).
- **Alpha Vantage API Key**: Free key from [Alpha Vantage](https://www.alphavantage.co/support/#api-key) (5 calls/minute limit).

## Installation
1. **Clone the Repository**:
   ```bash
   git clone https://github.com/your-username/stock-sentinel.git
   cd stock-sentinel
