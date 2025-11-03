# Quickstart Guide: Development Environment Setup

**Feature**: Disaster Social Media Analysis  
**Date**: 2025-10-31  
**Target Audience**: Developers

## Overview

This guide helps developers set up the development environment for the Disaster
Social Media Analysis application. Estimated setup time: **20-30 minutes**.

---

## Prerequisites

### Required

- **Operating System**: Windows 10/11, macOS 11+, or Linux (Ubuntu 20.04+)
- **RAM**: 8 GB minimum (16 GB recommended for running Python API)
- **Disk Space**: 2 GB for Java tooling, 5 GB if using Python ML models
- **Internet Connection**: Required for dependency downloads

### Optional (for Python ML-based Analysis)

- **Python 3.11+**: Only if using ML-based sentiment analysis
- **GPU**: NVIDIA GPU with CUDA support (optional, improves analysis speed)

---

## Step 1: Install Java Development Kit (JDK)

### Option A: Eclipse Temurin (Recommended)

**Why**: Free, open-source, LTS support through 2029

**Installation**:

1. Download JDK 25 from
   [Oracle](https://www.oracle.com/java/technologies/downloads/#java25) or
   [Adoptium](https://adoptium.net/temurin/releases/?version=25)
2. Select your OS and architecture (x64 for most systems)
3. Install using the downloaded installer
4. Verify installation:

```bash
# Windows (PowerShell)
java -version

# macOS/Linux
java --version
```

**Expected Output**:

```text
openjdk version "17.0.9" 2023-10-17 LTS
OpenJDK Runtime Environment Temurin-17.0.9+9 (build 17.0.9+9-LTS)
```

### Option B: Oracle JDK

Download from
[Oracle](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)
(requires Oracle account)

### Environment Variables (Windows)

If `java -version` fails, set JAVA_HOME:

1. Search "Environment Variables" in Start Menu
2. Click "Environment Variables..."
3. Add new system variable:
   - **Name**: `JAVA_HOME`
   - **Value**: `C:\Program Files\Eclipse Adoptium\jdk-17.0.9.9-hotspot` (adjust
     path)
4. Edit `Path` variable, add: `%JAVA_HOME%\bin`
5. Restart PowerShell and verify

---

## Step 2: Install Apache Maven

### Windows

**Option A: Chocolatey Package Manager**

```powershell
# Install Chocolatey if not already installed
Set-ExecutionPolicy Bypass -Scope Process -Force; [System.Net.ServicePointManager]::SecurityProtocol = [System.Net.ServicePointManager]::SecurityProtocol -bor 3072; iex ((New-Object System.Net.WebClient).DownloadString('https://community.chocolatey.org/install.ps1'))

# Install Maven
choco install maven -y
```

**Option B: Manual Installation**

1. Download Maven 3.9.x from
   [Apache Maven](https://maven.apache.org/download.cgi)
2. Extract to `C:\Program Files\Apache\maven`
3. Add to Path:
   - Add system variable `MAVEN_HOME` → `C:\Program Files\Apache\maven`
   - Edit `Path`, add `%MAVEN_HOME%\bin`

**Verify**:

```powershell
mvn -version
```

**Expected Output**:

```text
Apache Maven 3.9.11
Maven home: C:\Program Files\Apache\maven
Java version: 25.0.0, vendor: Oracle Corporation (or Eclipse Adoptium)
```

### macOS

```bash
# Using Homebrew
brew install maven

# Verify
mvn -version
```

### Linux

```bash
# Ubuntu/Debian
sudo apt update
sudo apt install maven

# Fedora/RHEL
sudo dnf install maven

# Verify
mvn -version
```

---

## Step 3: Install Git (Version Control)

### Windows

Download from [git-scm.com](https://git-scm.com/download/win) and install with
default options.

### macOS

```bash
brew install git
```

### Linux

```bash
sudo apt install git  # Ubuntu/Debian
sudo dnf install git  # Fedora/RHEL
```

**Verify**:

```bash
git --version
```

---

## Step 4: Clone the Repository

```bash
# Clone the project
git clone https://github.com/your-org/yagi-social-analyzer.git

# Navigate to project directory
cd yagi-social-analyzer

# Checkout feature branch (if working on disaster analysis feature)
git checkout 001-disaster-analysis
```

---

## Step 5: Build the Project

```bash
# Clean and compile
mvn clean compile

# Run tests (may take 2-3 minutes)
mvn test

# Package application (creates JAR file)
mvn package
```

**Expected Output**:

```text
[INFO] BUILD SUCCESS
[INFO] Total time: 45.123 s
[INFO] Finished at: 2024-09-15T10:30:00+07:00
```

**Generated Artifacts**:

- `target/yagi-social-analyzer-1.0.0.jar` - Application JAR
- `target/yagi-social-analyzer-1.0.0-jar-with-dependencies.jar` - Executable JAR
  with all dependencies

---

## Step 6: Run the Application

### Using Maven

```bash
mvn javafx:run
```

### Using Java Directly

```bash
java -jar target/yagi-social-analyzer-1.0.0-jar-with-dependencies.jar
```

**Expected Behavior**:

- JavaFX window opens with main UI
- Console logs show application initialization
- Database file created at `data/application.db`

---

## Step 7: IDE Setup (Optional but Recommended)

### IntelliJ IDEA (Recommended)

1. Download
   [IntelliJ IDEA Community Edition](https://www.jetbrains.com/idea/download/)
   (free)
2. Install and open IntelliJ
3. Click "Open" and select project folder
4. IntelliJ auto-detects Maven project and downloads dependencies
5. Enable JavaFX support:
   - File → Settings → Plugins → Search "JavaFX" → Install plugin
6. Run application:
   - Right-click `src/main/java/com/yagi/socialanalyzer/Main.java` → Run

### VS Code

1. Install [VS Code](https://code.visualstudio.com/)
2. Install extensions:
   - **Extension Pack for Java** (Microsoft)
   - **JavaFX Support** (VsCode JavaFX)
3. Open project folder
4. VS Code detects Maven and prompts to install dependencies
5. Run via Debug panel or terminal: `mvn javafx:run`

### Eclipse

1. Download
   [Eclipse IDE for Java Developers](https://www.eclipse.org/downloads/)
2. Install and launch
3. File → Import → Maven → Existing Maven Projects
4. Select project folder
5. Install e(fx)clipse plugin:
   - Help → Eclipse Marketplace → Search "e(fx)clipse" → Install
6. Run: Right-click project → Run As → Java Application

---

## Step 8: Configure Application (First Run)

### Platform Credentials Setup

1. Launch application
2. Navigate to **Settings → Platform Credentials**
3. Add credentials for platforms you want to use:

   **Twitter/X**:

   - API Key, API Secret, Access Token, Access Token Secret
   - Get from [Twitter Developer Portal](https://developer.twitter.com/)

   **Reddit**:

   - Client ID, Client Secret
   - Get from [Reddit App Preferences](https://www.reddit.com/prefs/apps)

   **YouTube**:

   - API Key
   - Get from [Google Cloud Console](https://console.cloud.google.com/)

4. Credentials are encrypted and stored in OS keyring

### Damage Categories (Optional Customization)

1. Navigate to **Settings → Damage Categories**
2. Pre-configured categories:
   - Affected People
   - Economic Disruption
   - Damaged Buildings
   - Lost Property
   - Damaged Infrastructure
   - Other
3. Add/edit keywords in Vietnamese or English
4. Changes saved to `config/damage_categories.yaml`

---

## Step 9: Create Your First Project

1. Click **New Project** button
2. Fill in details:
   - **Name**: Typhoon Yagi Analysis
   - **Disaster Name**: Typhoon Yagi
   - **Region**: Northern Vietnam
   - **Date Range**: 2024-09-01 to 2024-09-15
   - **Keywords**: Typhoon Yagi, Bão Yagi, Vietnam flood, Hanoi flood
   - **Data Sources**: Select Twitter, Reddit, YouTube
3. Click **Create**
4. Click **Start Collection**
5. Monitor progress in collection tab
6. When complete, click **Run Analysis**
7. View results in charts and export reports

---

## Optional: Python ML Sentiment Analysis Setup

**Only follow this section if you want ML-based sentiment analysis (85-90%
accuracy) instead of Java lexicon (70-75% accuracy).**

### Prerequisites

- Python 3.11+
- pip package manager

### Installation

```bash
# Navigate to Python API directory
cd python-analysis-api

# Create virtual environment
python -m venv venv

# Activate virtual environment
# Windows
venv\Scripts\activate

# macOS/Linux
source venv/bin/activate

# Install dependencies
pip install -r requirements.txt
```

**requirements.txt**:

```text
fastapi==0.104.1
uvicorn[standard]==0.24.0
transformers==4.35.0
torch==2.1.0
pydantic==2.5.0
```

### Download Models

```bash
# First run downloads models (~1 GB)
python -c "from transformers import pipeline; pipeline('sentiment-analysis', model='vinai/phobert-base-v2'); pipeline('sentiment-analysis', model='roberta-base')"
```

### Start API Server

```bash
uvicorn main:app --host 127.0.0.1 --port 8000 --workers 4
```

**Expected Output**:

```text
INFO:     Started server process [12345]
INFO:     Waiting for application startup.
INFO:     Application startup complete.
INFO:     Uvicorn running on http://127.0.0.1:8000
```

### Test API

```bash
# Health check
curl http://localhost:8000/health

# Expected response
{"status":"healthy","models_loaded":{"vietnamese":true,"english":true},"version":"1.0.0"}
```

### Configure Java Application

1. Open application
2. Navigate to **Settings → Analysis Configuration**
3. Change **Sentiment Backend** from "Java Lexicon" to "Python API"
4. Enter **Python API URL**: `http://localhost:8000`
5. Click **Test Connection** to verify
6. Save settings

---

## Troubleshooting

### Issue: `mvn` command not found

**Solution**: Maven not in PATH. Re-add `%MAVEN_HOME%\bin` to Path variable and
restart terminal.

### Issue: JavaFX runtime error on startup

**Error**: `Error: JavaFX runtime components are missing`

**Solution**: Ensure `pom.xml` has `javafx-maven-plugin` configured. Run
`mvn clean install` again.

### Issue: SQLite database locked

**Error**: `SQLite database is locked`

**Solution**: Close all application instances. Delete `data/application.db.lock`
file if exists.

### Issue: Python API not reachable

**Error**: `Connection refused to localhost:8000`

**Solution**:

1. Verify Python API is running: `curl http://localhost:8000/health`
2. Check firewall settings (allow port 8000)
3. Try `127.0.0.1` instead of `localhost` in configuration

### Issue: Out of memory during analysis

**Error**: `java.lang.OutOfMemoryError: Java heap space`

**Solution**: Increase heap size:

```bash
java -Xmx4g -jar target/yagi-social-analyzer-1.0.0-jar-with-dependencies.jar
```

Or in `pom.xml`, add to `javafx-maven-plugin`:

```xml
<configuration>
  <options>
    <option>-Xmx4g</option>
  </options>
</configuration>
```

### Issue: Checkstyle/SpotBugs errors

**Error**: Build fails due to code quality checks

**Solution**:

```bash
# Skip code quality checks temporarily
mvn clean package -DskipTests -Dcheckstyle.skip -Dspotbugs.skip

# Fix issues and re-enable checks
mvn checkstyle:check
mvn spotbugs:check
```

---

## Next Steps

### For Developers

1. Read `docs/architecture.md` for system design overview
2. Review `specs/001-disaster-analysis/data-model.md` for entity details
3. Check `specs/001-disaster-analysis/contracts/interfaces.md` for service
   interfaces
4. Run test suite: `mvn test`
5. Generate Javadoc: `mvn javadoc:javadoc` (output in `target/site/apidocs`)

### For Users

1. Refer to `docs/user-guide.md` for detailed feature documentation
2. Watch tutorial video: [YouTube Link]
3. Join community: [Discord Server]

### For Contributors

1. Read `CONTRIBUTING.md` for code style guidelines
2. Create feature branch: `git checkout -b feature/your-feature-name`
3. Make changes and commit: `git commit -m "feat: add damage visualization"`
4. Push and create pull request

---

## System Requirements Summary

| Component             | Minimum                            | Recommended                        |
| --------------------- | ---------------------------------- | ---------------------------------- |
| **Java**              | JDK 25                             | JDK 25                             |
| **Maven**             | 3.8.x                              | 3.9.x                              |
| **RAM**               | 8 GB                               | 16 GB                              |
| **Disk**              | 2 GB                               | 5 GB (with Python)                 |
| **OS**                | Windows 10, macOS 11, Ubuntu 20.04 | Windows 11, macOS 13, Ubuntu 22.04 |
| **Python** (optional) | 3.11                               | 3.11+                              |

---

## Additional Resources

- **JavaFX Documentation**: [https://openjfx.io/](https://openjfx.io/)
- **Maven Guide**:
  [https://maven.apache.org/guides/](https://maven.apache.org/guides/)
- **SQLite Documentation**:
  [https://www.sqlite.org/docs.html](https://www.sqlite.org/docs.html)
- **Selenium WebDriver**:
  [https://www.selenium.dev/documentation/](https://www.selenium.dev/documentation/)
- **FastAPI (Python)**:
  [https://fastapi.tiangolo.com/](https://fastapi.tiangolo.com/)

---

## Summary

You have successfully:

✅ Installed Java 25 and Maven 3.9+  
✅ Cloned and built the project  
✅ Configured your IDE  
✅ Created your first disaster analysis project  
✅ (Optional) Set up Python ML sentiment analysis API

**Total Setup Time**: 20-30 minutes (excluding model downloads)

For questions or issues, open an issue on GitHub or contact the development
team.
