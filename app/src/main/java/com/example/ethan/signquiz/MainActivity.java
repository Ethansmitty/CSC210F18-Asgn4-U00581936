package com.example.ethan.signquiz;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.res.AssetManager;
import android.graphics.drawable.Drawable;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import android.os.Handler;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "SignQuiz Activity";

    private static final int SIGNS_IN_QUIZ = 9;

    private List<String> fileNameList; // flag file names
    private List<String> quizSignsList; // signs in current quiz
    private String correctAnswer; // correct meaning for the current sign
    private int totalGuesses; // number of guesses made
    private int correctAnswers; // number of correct guesses
    private int guessRows = 4; // number of rows displaying guess Buttons
    private SecureRandom random; // used to randomize the quiz
    private Handler handler;
    private LinearLayout quizLinearLayout; // layout that contains the quiz
    private ImageView signImageView; // displays a flag
    private LinearLayout[] guessLinearLayouts; // rows of answer Buttons
    private TextView answerTextView; // displays correct answer

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fileNameList = new ArrayList<>();
        quizSignsList = new ArrayList<>();
        random = new SecureRandom();
        handler = new Handler();

        // get references to GUI components
        quizLinearLayout = findViewById(R.id.quizLinearLayout);
        signImageView = findViewById(R.id.signImageView);
        guessLinearLayouts = new LinearLayout[4];
        guessLinearLayouts[0] = findViewById(R.id.row1LinearLayout);
        guessLinearLayouts[1] = findViewById(R.id.row2LinearLayout);
        guessLinearLayouts[2] = findViewById(R.id.row3LinearLayout);
        guessLinearLayouts[3] = findViewById(R.id.row4LinearLayout);
        answerTextView = findViewById(R.id.answerTextView);

        // configure listeners for the guess Buttons
        for (LinearLayout row : guessLinearLayouts) {
            for (int column = 0; column < row.getChildCount(); column++) {
                Button button = (Button) row.getChildAt(column);
                button.setOnClickListener(guessButtonListener);
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        resetQuiz();
    }

    // set up and start the next quiz
    public void resetQuiz() {
        // use AssetManager to get image file names for signs
        AssetManager assets = getAssets();
        fileNameList.clear(); // empty list of image file names

        try {
            String[] paths = assets.list("signs");

            for (String path : paths) {
                fileNameList.add(path.replace(".jpg", ""));
            }
        } catch (IOException exception) {
            Log.e(TAG, "Error loading image file names", exception);
        }
        correctAnswers = 0; // reset the number of correct answers made
        totalGuesses = 0; // reset the total number of guesses the user made
        quizSignsList.clear(); // clear prior list of quiz countries

        int signCounter = 1;
        int numberOfSigns = fileNameList.size();

        // add SIGNS_IN_QUIZ random file names to the quizSignsList
        while (signCounter <= SIGNS_IN_QUIZ) {
            int randomIndex = random.nextInt(numberOfSigns);

            // get the random file name
            String filename = fileNameList.get(randomIndex);

            // if it hasn't already been chosen
            if (!quizSignsList.contains(filename)) {
                quizSignsList.add(filename); // add the file to the list
                ++signCounter;
            }
        }

        loadNextSign(); // start the quiz by loading the first sign
    }

    // after the user guesses a correct sign, load the next sign
    private void loadNextSign() {
        // get file name of the next flag and remove it from the list
        String nextImage = quizSignsList.remove(0);
        correctAnswer = nextImage; // update the correct answer
        answerTextView.setText(""); // clear answerTextView

        // use AssetManager to load next image from assets folder
        AssetManager assets = getAssets();

        // get an InputStream to the asset representing the next flag
        // and try to use the InputStream
        try (InputStream stream =
                     assets.open("signs" + "/" + nextImage + ".jpg")) {
            // load the asset as a Drawable and display on the flagImageView
            Drawable sign = Drawable.createFromStream(stream, nextImage);
            signImageView.setImageDrawable(sign);

            animate(false);
        }
        catch (IOException exception) {
            Log.e(TAG, "Error loading " + nextImage, exception);
        }

        Collections.shuffle(fileNameList); // shuffle file names

        // put the correct answer at the end of fileNameList
        int correct = fileNameList.indexOf(correctAnswer);
        fileNameList.add(fileNameList.remove(correct));

        // add 2, 4, 6 or 8 guess Buttons based on the value of guessRows
        for (int row = 0; row < guessRows; row++) {
            // place Buttons in currentTableRow
            for (int column = 0; column < guessLinearLayouts[row].getChildCount(); column++) {
                // get reference to Button to configure
                Button newGuessButton =
                        (Button) guessLinearLayouts[row].getChildAt(column);
                newGuessButton.setEnabled(true);

                // get country name and set it as newGuessButton's text
                String filename = fileNameList.get((row * 2) + column);
                newGuessButton.setText(getSignName(filename));
            }
        }

        // randomly replace one Button with the correct answer
        int row = random.nextInt(guessRows); // pick random row
        int column = random.nextInt(2); // pick random column
        LinearLayout randomRow = guessLinearLayouts[row]; // get the row
        String countryName = getSignName(correctAnswer);
        ((Button) randomRow.getChildAt(column)).setText(countryName);
    }

    private String getSignName(String name) {
        return name.substring(name.indexOf('-') + 1).replace('_', ' ');
    }

    // animates the entire quizLinearLayout on or off screen
    private void animate(boolean animateOut) {
        // prevent animation into the the UI for the first flag
        if (correctAnswers == 0)
            return;

        // calculate center x and center y
        int centerX = (quizLinearLayout.getLeft() +
                quizLinearLayout.getRight()) / 2; // calculate center x
        int centerY = (quizLinearLayout.getTop() +
                quizLinearLayout.getBottom()) / 2; // calculate center y

        // calculate animation radius
        int radius = Math.max(quizLinearLayout.getWidth(),
                quizLinearLayout.getHeight());

        Animator animator;

        // if the quizLinearLayout should animate out rather than in
        if (animateOut) {
            // create circular reveal animation
            animator = ViewAnimationUtils.createCircularReveal(
                    quizLinearLayout, centerX, centerY, radius, 0);
            animator.addListener(
                    new AnimatorListenerAdapter() {
                        // called when the animation finishes
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            loadNextSign();
                        }
                    }
            );
        }
        else { // if the quizLinearLayout should animate in
            animator = ViewAnimationUtils.createCircularReveal(
                    quizLinearLayout, centerX, centerY, 0, radius);
        }

        animator.setDuration(500); // set animation duration to 500 ms
        animator.start(); // start the animation
    }

    // called when a guess Button is touched
    private View.OnClickListener guessButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            Button guessButton = ((Button) v);
            String guess = guessButton.getText().toString();
            String answer = getSignName(correctAnswer);
            ++totalGuesses; // increment number of guesses the user has made

            if (guess.equals(answer)) { // if the guess is correct
                ++correctAnswers; // increment the number of correct answers

                // display correct answer in green text
                answerTextView.setText(answer + "!");
                answerTextView.setTextColor(getResources().getColor(R.color.correct_answer, getTheme()));

                disableButtons(); // disable all guess Buttons

                // if the user has correctly identified FLAGS_IN_QUIZ flags
                if (correctAnswers == SIGNS_IN_QUIZ) {



                } else { // answer is correct but quiz is not over
                    // load the next sign after a 2-second delay
                    handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    animate(true);
                                }
                            }, 2000); // 2000 milliseconds for 2-second delay
                }
            } else { // answer was incorrect
                // display "Incorrect!" in red
                answerTextView.setText(R.string.incorrect_answer);
                answerTextView.setTextColor(getResources().getColor(
                        R.color.incorrect_answer, getTheme()));
                guessButton.setEnabled(false); // disable incorrect answer
            }

        }

    };


    // utility method that disables all answer Buttons
    private void disableButtons() {
        for (int row = 0; row < guessRows; row++) {
            LinearLayout guessRow = guessLinearLayouts[row];
            for (int i = 0; i < guessRow.getChildCount(); i++)
                guessRow.getChildAt(i).setEnabled(false);
        }
    }

}
