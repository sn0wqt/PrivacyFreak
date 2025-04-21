# PrivacyFreak Bot

<p align="center">
  <img src="assets/logo.png" alt="Privacy Freak Bot Logo" width="200" height="200">
</p>

<p align="center">
  <em>A Telegram bot that helps protect your privacy by inspecting and removing metadata from your files</em>
</p>

#### Video Demo: 

<p align="center">
  <a href="https://youtu.be/3wdisua1wrY">
    <img src="https://img.youtube.com/vi/3wdisua1wrY/maxresdefault.jpg" alt="Privacy Freak Bot Demo" width="600">
  </a>
  <br>
  <em>Click to watch the demo video</em>
</p>

#### Description:

The **Privacy Freak Bot** is a **Telegram bot** designed to help protect your privacy by inspecting and removing metadata from media files. Built with Java, it allows you to easily examine what hidden information might be embedded in your images or videos, and strip that metadata from JPEGs to ensure your privacy when sharing files. No more accidentally leaking location data, device information, or other sensitive details!

---

### **Features**

- **Inspect Metadata:**
  Use the `/metadata` command to examine hidden information in your files. The bot returns a comprehensive report of all metadata found, including camera details, location data, timestamps, and software information.

- **Strip Metadata:**
  With the `/strip` command, send any JPEG image and receive a clean version with all metadata removed, including EXIF, XMP, ICC profiles, and JFIF headers.

- **Support for Multiple Formats:**
  View metadata from a wide range of file types, including common image formats (JPEG, PNG, GIF), RAW camera files (ARW, NEF, CR2), and video/audio formats (MP4, MOV, MP3).

- **Privacy-Focused:**
  Files are processed in memory and not stored permanently, ensuring your sensitive data never persists on the server.

- **User-Friendly Commands:**
  Simple command structure with clear instructions and helpful error messages.

- **Help Command:**
  The `/help` command provides detailed information about supported formats and how to use the bot effectively.

---

### **Project Structure**

The project is written in Java and follows a clean architecture with separation of concerns:

1. **Main Package (`com.sn0wqt.privacyfreak`)**
   - `App.java`: The entry point that initializes the Telegram bot.
   - `Bot.java`: Core class that handles the Telegram bot's main functionality.

2. **Commands Package (`commands`)**
   - `CommandHandler.java`: Processes user commands and delegates to appropriate services.

3. **Config Package (`config`)**
   - `Config.java`: Contains configuration constants like supported file formats and command definitions.

4. **Services Package (`services`)**
   - `FileService.java`: Handles downloading files from Telegram.
   - `MediaHandler.java`: Processes media files for metadata extraction and stripping.
   - `MessageService.java`: Manages sending messages and files back to users.

5. **State Package (`state`)**
   - `UserState.java`: Maintains user session state to handle multi-step operations.

6. **Utils Package (`utils`)**
   - `FileUtils.java`: Utility methods for file operations.
   - `MetadataUtils.java`: Core functionality for metadata extraction and stripping.

---

### **Design Choices**

1. **Clean Architecture:**
   The project follows a service-oriented design pattern with clear separation of concerns, making the codebase maintainable and testable.

2. **In-Memory Processing:**
   All file processing happens in memory to ensure user privacy and minimize storage requirements.

3. **Extensive Format Support:**
   For metadata extraction, the bot supports a wide range of formats by leveraging the powerful `metadata-extractor` library.

4. **Metadata Stripping Strategy:**
   For JPEG images, the bot uses Apache Commons Imaging to systematically remove multiple metadata types (EXIF, XMP, IPTC, ICC profiles) while preserving image quality.

5. **User State Management:**
   The bot implements a state management system to handle the flow of conversation, allowing for commands that expect follow-up file uploads.

---

### **Technologies Used**

- **Java**: Core programming language
- **Telegram Bot API**: For bot functionality
- **Apache Commons Imaging**: For metadata manipulation
- **Drew Noakes' metadata-extractor**: For comprehensive metadata extraction
- **TelegramBots Library**: Java framework for Telegram bot development

---

### **Future Improvements**

- **Video Metadata Stripping:**
  Add support for removing metadata from video files.

- **Batch Processing:**
  Allow users to strip metadata from multiple files at once.

- **Custom Stripping Options:**
  Let users choose which specific metadata types they want to remove.

- **Image Compression:**
  Offer additional privacy features like image compression to further remove identifying characteristics.

- **Privacy Analysis:**
  Provide a privacy risk score based on the sensitivity of detected metadata.

---

### **How to Run the Project**

1. **Clone the Repository**
   - Open your terminal and run:

     ```bash
     git clone https://github.com/sn0wqtokie/privacyfreak.git
     cd privacyfreak
     ```

2. **Configure Environment Variables**
   - Create a `.env` file in the project root and add your Telegram bot token:

     ```env
     BOT_TOKEN="<Your_Telegram_Bot_Token>"
     ```

3. **Build with Maven**
   - Ensure you have Maven installed and run:

     ```bash
     mvn clean package
     ```

4. **Run the Bot**
   - Launch it with:
   
     ```
     java -jar .\target\privacy_freak-0.1.1-jar-with-dependencies.jar
     ```

5. **Start Using the Bot**
   - Open Telegram and search for your bot by username
   - Start a conversation with the bot by sending `/start`
   - Use `/help` to see all available commands and supported formats

---

### **About Privacy**

This bot was created with a strong focus on user privacy. Unlike many online tools that might store or analyze your data, Privacy Freak Bot processes everything locally and temporarily. Your files are never stored permanently, ensuring that your sensitive information remains secure.

Remember that metadata can reveal more than you think - from exact GPS coordinates where a photo was taken to the serial number of your device. Being mindful about what data your files contain before sharing them is an important step in protecting your digital privacy.

Created by: @sn0wqt 2025
