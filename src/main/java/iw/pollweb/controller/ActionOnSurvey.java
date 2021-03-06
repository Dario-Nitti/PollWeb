/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package iw.pollweb.controller;

import iw.framework.data.DataException;
import iw.framework.result.SplitSlashesFmkExt;
import iw.framework.result.TemplateManagerException;
import iw.framework.result.TemplateResult;
import iw.framework.security.SecurityLayer;
import static iw.framework.security.SecurityLayer.checkSession;
import iw.framework.utils.EmailSender;
import iw.framework.utils.PasswordUtility;
import iw.pollweb.model.PollWebDataLayer;
import iw.pollweb.model.dto.Choice;
import iw.pollweb.model.dto.Participant;
import iw.pollweb.model.dto.Question;
import iw.pollweb.model.dto.Supervisor;
import iw.pollweb.model.dto.Survey;
import java.io.IOException;
import java.io.PrintWriter;
import java.rmi.ServerException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 *
 * @author dario
 */
public class ActionOnSurvey extends BaseController {

    private void action_default(HttpServletRequest request, HttpServletResponse response) throws ServerException, TemplateManagerException, DataException {
        HttpSession s = SecurityLayer.checkSession(request);
        int SurveyID = SecurityLayer.checkNumeric(request.getParameter("id"));
        Survey survey = ((PollWebDataLayer) request.getAttribute("datalayer")).getSurveyDAO().getSurveyByID(SurveyID);
        request.setAttribute("survey", survey);
        List<Question> questions = ((PollWebDataLayer) request.getAttribute("datalayer")).getQuestionDAO().getQuestionsBySurvey(survey);
        request.setAttribute("questions", questions);
        TemplateResult res = new TemplateResult(getServletContext());
        request.setAttribute("split_shalshes", new SplitSlashesFmkExt());
        request.setAttribute("page_title", "login");
        res.activate("/modifica-sondaggio.ftl.html", request, response);

    }

    private void action_delete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, DataException {
        int surveyID = SecurityLayer.checkNumeric(request.getParameter("pollID"));
        Survey survey = ((PollWebDataLayer) request.getAttribute("datalayer")).getSurveyDAO().getSurveyByID(surveyID);
        if (survey != null) {
            response.sendRedirect("/PollWeb/profile");
            ((PollWebDataLayer) request.getAttribute("datalayer")).getSurveyDAO().deleteSurvey(surveyID);
        } else {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    private void action_close(HttpServletRequest request, HttpServletResponse response) throws ServerException, IOException, DataException {
        Survey survey = ((PollWebDataLayer) request.getAttribute("datalayer")).getSurveyDAO().getSurveyByID(Integer.parseInt(request.getParameter("pollID")));
        if (survey != null) {
            survey.setActive(true);
            ((PollWebDataLayer) request.getAttribute("datalayer")).getSurveyDAO().storeSurvey(survey);
            response.sendRedirect("/PollWeb/profile");
        } else {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
        }

    }

    private void action_deactivate(HttpServletRequest request, HttpServletResponse response) throws ServerException, DataException, IOException {
        Survey survey = ((PollWebDataLayer) request.getAttribute("datalayer")).getSurveyDAO().getSurveyByID(Integer.parseInt(request.getParameter("pollID")));
        if (survey != null) {
            survey.setClosed(true);
            ((PollWebDataLayer) request.getAttribute("datalayer")).getSurveyDAO().storeSurvey(survey);
            response.sendRedirect("/PollWeb/profile");
        }
    }

    private void action_addPart(HttpServletRequest request, HttpServletResponse response, HttpSession s) throws ServletException, DataException, IOException {
        //prendo il sondaggio appena creato attraverso il suo id

        int surveyID = SecurityLayer.checkNumeric(request.getParameter("pollID"));
        Survey survey = ((PollWebDataLayer) request.getAttribute("datalayer")).getSurveyDAO().getSurveyByID(surveyID);
        Participant participant;
        participant = ((PollWebDataLayer) request.getAttribute("datalayer")).getParticipantDAO().createParticipant();

        String FName = request.getParameter("firstName");
        String LName = request.getParameter("lastName");
        String email = request.getParameter("email");
        String password = PasswordUtility.generateRandomPassword();
        //creo un nuovo partecipante
        //setto i valori nel database
        if (email != null && FName != null && LName != null) {
            participant.setFirstName(FName);
            participant.setLastName(LName);
            participant.setEmail(email);
            participant.setHashedPassword(PasswordUtility.getSHA256(password));
            participant.setSurvey(survey);
            if (survey.getParticipants().contains(participant)) {
                throw new ServletException("Il participante è già registrato.");
            }
            String mittente = "pollweb2020@gmail.com";
            String pass = "We_PollWeb_2020";
            String obj = "C'è un sondaggio per te";
            String url = "http://localhost:8080/PollWeb/login";
            String testo = "Ciao " + FName + " " + LName + "\n"
                    + "Sei stato invitato ad un nuovo sondaggio le tue credenziali sono: \n\n" + "Email:  " + email + "\n" + "password:  " + password + "\n" + "clicca qui per accedere al sondaggio: " + url;
            EmailSender.send(mittente, pass, email, obj, testo);
            response.sendRedirect("/PollWeb/actionsurvey?id=" + survey.getID());

            ((PollWebDataLayer) request.getAttribute("datalayer")).getParticipantDAO().storeParticipant(participant);
        }
    }

    private void action_question(HttpServletRequest request, HttpServletResponse response) throws ServletException, DataException, IOException {
        int surveyID = SecurityLayer.checkNumeric(request.getParameter("pollID"));
        Survey survey = ((PollWebDataLayer) request.getAttribute("datalayer")).getSurveyDAO().getSurveyByID(surveyID);
        Question question;
        question = ((PollWebDataLayer) request.getAttribute("datalayer")).getQuestionDAO().createQuestion();

        String type = request.getParameter("type");
        switch (type) {
            case "Lunga":
                type = "long";
                break;
            case "Corta":
                type = "short";
                break;
            case "Data":
                type = "date";
                break;
            case "Numero":
                type = "number";
                break;
            case "Risposta chiusa con scelta singola":
                type = "single";
                break;
            case "Risposta chiusa con scelta multipla":
                type = "multiple";
                break;
        }
        String text = request.getParameter("text");
        String note = request.getParameter("note");
        boolean mandatory = request.getParameter("isMandatory") != null;
//        boolean private = request.getParameter("isPrivate")!= null;
        int number = Integer.parseInt(request.getParameter("number"));
        if (type != null) {
            question.setType(type);
            question.setNote(note);
            question.setText(text);
            question.setMandatory(mandatory);
            question.setNumber(number);
            question.setSurvey(survey);
                  response.sendRedirect("/PollWeb/actionsurvey?id=" + survey.getID());

            ((PollWebDataLayer) request.getAttribute("datalayer")).getQuestionDAO().storeQuestion(question);

//            if (type.equals("multiple") || type.equals("single")) {
//                Choice choice;
//
//                choice = ((PollWebDataLayer) request.getAttribute("datalayer")).getChoiceDAO().createChoice();
//
//                String[] choiceValue = request.getParameterValues("value");
//                List<Choice> choicesList = new ArrayList<Choice>();
//

//                    ((PollWebDataLayer) request.getAttribute("datalayer")).getChoiceDAO().storeChoices(choicesList);
                }

            }

        

    

    @Override
    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, DataException {
        try {
            HttpSession s = checkSession(request);

            if (request.getParameter("deactivate") != null) {
                action_deactivate(request, response);
            } else if (request.getParameter("delete") != null) {
                action_delete(request, response);
            } else if (request.getParameter("close") != null) {
                action_close(request, response);
            } else if (request.getParameter("addpart") != null) {
                action_addPart(request, response, s);
            } else if (request.getParameter("question") != null) {
                action_question(request, response);

            } else {
                action_default(request, response);
            }

        } catch (Exception e) {
        }
    }

    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>
}
