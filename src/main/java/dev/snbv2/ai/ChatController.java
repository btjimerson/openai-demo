package dev.snbv2.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

import jakarta.servlet.http.HttpSession;

/**
 * Main application controller.
 * 
 * @author Brian Jimerson
 */
@Controller
@SessionAttributes({ "chatHistory" })
public class ChatController {

    @Autowired
    @Qualifier("openAiChatClient")
    ChatClient openAiChatClient;

    @Autowired
    @Qualifier("restClientBuilder")
    RestClient.Builder restClientBuilder;

    @Value("${openai.api-key}")
    String openAiApiKey;

    @Value("${openai.base-url}")
    String openAiBaseUrl;

    /**
     * Default view controller.
     * 
     * @param model   The model to use for the view.
     * @param session The HTTP session to use.
     * @return The view name to use (home.html).
     */
    @GetMapping(path = { "/", "/index" })
    public String index(Model model, HttpSession session) {

        Chat chat = new Chat();
        model.addAttribute("chat", chat);
        setViewAttributes(model, session);

        return "home";
    }

    /**
     * Controller method to handle a chat prompt POST.
     * 
     * @param userPrompt The prompt entered by the user.
     * @param model      The model to use for the view.
     * @param session    The HTTP session to use.
     * @return The view name to use (home.html).
     */
    @PostMapping("/chat")
    public String chat(@ModelAttribute Chat userPrompt, Model model, HttpSession session) {

        setViewAttributes(model, session);
        ChatHistory chatHistory = (ChatHistory) session.getAttribute("chatHistory");
        chatHistory.addChatItem(userPrompt.getPrompt());

        ChatResponse chatResponse = openAiChatClient.prompt().user(userPrompt.getPrompt()).call().chatResponse();
        String chatContent = chatResponse.getResult().getOutput().getText();
        model.addAttribute("content", chatContent);
        model.addAttribute("chatPrompt", userPrompt.getPrompt());

        return "home";
    }

    /**
     * Controller method to update the API URL and key. The
     * openAiChatClient is recreated with the new URL and API key.
     * 
     * @param model   The model to use for the view
     * @param session The HTTP session to use
     * @return The view to use (home.html).
     */
    @PostMapping("/setApi")
    public String setApi(@RequestParam Map<String, String> body, Model model, HttpSession session) {

        OpenAiApi openAiApi = new OpenAiApi(body.get("openAiApiUrl"), body.get("openAiApiKey"), restClientBuilder, WebClient.builder());
        OpenAiChatOptions openAiChatOptions = OpenAiChatOptions.builder().model("gpt-4o").streamUsage(false).build();
        OpenAiChatModel openAiChatModel = new OpenAiChatModel(openAiApi, openAiChatOptions);
        ChatClient.Builder openAiClientBuilder = ChatClient.builder(openAiChatModel);
        openAiClientBuilder.defaultAdvisors(new ErrorHandlingAdvisor());
        openAiChatClient = openAiClientBuilder.build();

        setViewAttributes(model, session);
        session.setAttribute("openAiApiUrl", body.get("openAiApiUrl"));
        session.setAttribute("openAiApiKey", body.get("openAiApiKey"));

        return "home";

    }

    /**
     * Ensures that the attributes the view needs exist in the session and / or
     * model.
     * 
     * @param model   The model to use
     * @param session The HTTP session to verify.
     */
    private void setViewAttributes(Model model, HttpSession session) {
        if (session.getAttribute("chatHistory") == null) {
            session.setAttribute("chatHistory", new ChatHistory());
        }
        if (session.getAttribute("openAiApiUrl") == null) {
            session.setAttribute("openAiApiUrl", openAiBaseUrl);
        }
        if (session.getAttribute("openAiApiKey") == null) {
            session.setAttribute("openAiApiKey", openAiApiKey);
        }
    }

}
