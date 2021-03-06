package app.com.thetechnocafe.jkiosklibrary.Apis.SubjectFaculty;

import android.os.Handler;
import android.os.Looper;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.util.Map;

import app.com.thetechnocafe.jkiosklibrary.Apis.WebkioskCredentials;
import app.com.thetechnocafe.jkiosklibrary.Constants;
import app.com.thetechnocafe.jkiosklibrary.Exceptions.InvalidCredentialsException;
import app.com.thetechnocafe.jkiosklibrary.Contracts.ResultCallbackContract;
import app.com.thetechnocafe.jkiosklibrary.Utilities.CookieUtility;
import app.com.thetechnocafe.jkiosklibrary.Contracts.KioskContract;
import app.com.thetechnocafe.jkiosklibrary.Utilities.StringUtility;

/**
 * Created by gurleen on 6/6/17.
 */

public class KioskSubjectFaculty implements KioskContract<SubjectFacultyResult> {
    private ResultCallbackContract<SubjectFacultyResult> mCallback;
    private Handler mResultHandler;
    private static String URL = "https://webkiosk.jiit.ac.in/StudentFiles/Academic/StudSubjectFaculty.jsp";

    public KioskSubjectFaculty() {
        mResultHandler = new Handler(Looper.getMainLooper());
    }

    /*
    * Login in into www.webkiosk.jiit.ac.in
    * Get the cookies and hit the https://webkiosk.jiit.ac.in/StudentFiles/Academic/StudSubjectFaculty.jsp?x=&exam={semesterCode} url
    * to fetch the list of all subject faculty for the semester provided
    * */
    private KioskSubjectFaculty getSubjectFaculty(final String enrollmentNumber, final String dateOfBirth, final String password, final String college, final String url) {
        //Execute in different thread
        final Thread thread = new Thread() {
            @Override
            public void run() {
                super.run();

                try {
                    //Get the cookies from Webkiosk's website
                    Map<String, String> cookies = CookieUtility.getCookiesFor(enrollmentNumber, dateOfBirth, password, college);

                    //Login into webkiosk using the cookies
                    Document document = Jsoup.connect(url)
                            .cookies(cookies)
                            .userAgent(Constants.AGENT_MOZILLA)
                            .execute().parse();

                    //Create new subject faculty result
                    final SubjectFacultyResult subjectFacultyResult = new SubjectFacultyResult();

                    //Check if the returned web page contains the string "Session Timeout"
                    //if yes then login was unsuccessful
                    if (document.body().toString().toLowerCase().contains("session timeout")) {
                        //Throw invalid credentials exception
                        throw new InvalidCredentialsException();
                    } else {
                        //Traverse the html data to get the list of semesters
                        Elements elements = document.body()
                                .getElementsByTag("table")
                                .get(2).getElementsByTag("tbody")
                                .get(0).children();

                        //Traverse the list of subjects and parse them, while also
                        //cleaning up the text and removing occurrences of '&nbsp;'
                        //and remove ant occurrences of '\u00A0'
                        for (int x = 1; x < elements.size(); x++) {
                            //Create new SubjectFaculty object
                            SubjectFaculty subjectFaculty = new SubjectFaculty();

                            //Get the sub elements of each row
                            Elements subElements = elements.get(x).getElementsByTag("td");
                            subjectFaculty.setSubjectName(StringUtility.getSubjectName(subElements.get(1).text()));
                            subjectFaculty.setSubjectCode(StringUtility.getSubjectCode(subElements.get(1).text()));
                            subjectFaculty.setLectureFaculty(StringUtility.cleanString(subElements.get(2).text()));
                            subjectFaculty.setTutorialFaculty(StringUtility.cleanString(subElements.get(3).text()));
                            subjectFaculty.setPracticalFaculty(StringUtility.cleanString(subElements.get(4).text()));

                            //Add to subject faculty result
                            subjectFacultyResult.getSubjectFaculties().add(subjectFaculty);
                        }
                    }

                    //Check if user has provided a callback
                    if (mCallback != null) {
                        mResultHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                mCallback.onResult(subjectFacultyResult);
                            }
                        });
                    }

                } catch (final Exception e) {
                    e.printStackTrace();

                    //Check if callback is provided
                    if (mCallback != null) {
                        mResultHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                mCallback.onError(e);
                            }
                        });
                    }
                }
            }
        };
        thread.start();

        return this;
    }

    /*
    * Overloaded login method that takes WebkioskCredentials object
    * */
    public KioskSubjectFaculty getSubjectFaculty(WebkioskCredentials credentials, String semesterCode) {
        getSubjectFaculty(credentials.getEnrollmentNumber(), credentials.getDateOfBirth(), credentials.getPassword(),credentials.getCollege(), URL + Constants.URL_QUERY_PARAM + semesterCode);
        return this;
    }

    /*
    * Overloaded login method that takes WebkioskCredentials object and semester code
    * */
    public KioskSubjectFaculty getSubjectFaculty(WebkioskCredentials credentials) {
        getSubjectFaculty(credentials.getEnrollmentNumber(), credentials.getDateOfBirth(), credentials.getPassword(),credentials.getCollege(), URL);
        return this;
    }

    /*
     * Set a callback to get result from the login API
     */
    @Override
    public void addResultCallback(ResultCallbackContract<SubjectFacultyResult> callback) {
        this.mCallback = callback;
    }

    /*
    * Remove the provided callback by the user if result is no longer required
    * */
    @Override
    public void removeCallback() {
        this.mCallback = null;
    }
}
