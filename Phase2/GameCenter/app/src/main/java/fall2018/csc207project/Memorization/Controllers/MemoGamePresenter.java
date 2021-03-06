package fall2018.csc207project.Memorization.Controllers;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;

import fall2018.csc207project.Memorization.Models.MemoManager;
import fall2018.csc207project.Memorization.Models.MemoScore;
import fall2018.csc207project.Memorization.Models.MemoTile;
import fall2018.csc207project.Memorization.Views.MemoGameView;
import fall2018.csc207project.Models.CalculatorFactory;
import fall2018.csc207project.Models.DatabaseUtil;
import fall2018.csc207project.Models.ScoreCalculator;
import fall2018.csc207project.Models.ScoreManager;

/**
 * Class represents the implementation of the game logic.
 */
public class MemoGamePresenter implements GamePresenter {

    /**
     * The current user on the game.
     */
    private String currentUser;

    /**
     * Tells weather the score is recorded.
     */
    private boolean isScoreRecorded;
    /**
     *
     */
    private ScoreManager<MemoScore> scoreManager;
    /**
     * The number of taps that to click correct.
     */
    private int successTap;

    /**
     * The MemoManager that manage the Memo game.
     */
    private MemoManager memoManager;

    /**
     * Whether the hint is available.
     */
    private boolean isAvailableHint;

    /**
     * The View of the Memo Game.
     */
    private MemoGameView view;

    /**
     * The Iterator<MemoTile> that let you iterate throw all MemoTiles.
     */
    private Iterator<MemoTile> verifyIterator;

    /**
     * The next MemoTile to get verifies.
     */
    private MemoTile nextToVerify;

    /**
     * Tells whether the user can tap on the tile
     * or not if it is still displaying.
     */
    private boolean isDisplaying;

    /**
     * Number of life left for the current user.
     */
    private int life;

    /**
     * The delay of each flashing between each tiles.
     */
    private int flashDelay = 1000;

    /**
     * Tells weather the game is stop.
     */
    private boolean gameOver;

    /**
     * Construct a new MemoGamePresenter
     * by given a final MemoGameView and a Context.
     *
     * @param view the MemoGame's view
     * @param context the context of the app
     */
    @SuppressWarnings("unused")
    public MemoGamePresenter(final MemoGameView view, Context context) {
        this.view = view;
        SharedPreferences shared
                = context.getSharedPreferences("GameData", Context.MODE_PRIVATE);
        ScoreCalculator<MemoScore> calculator =
                new CalculatorFactory().getCalculator("MemoCalculator");
        currentUser = shared.getString("currentUser", null);
        String game = shared.getString("currentGame", null);
        scoreManager = DatabaseUtil.getScoreManager(game, currentUser, calculator);
        setupStates();
        view.updateScore(successTap);
        view.updateLife(life);
    }

    /**
     * Setup the game states.
     */
    private void setupStates(){
        isAvailableHint = true;
        isDisplaying = false;
        gameOver = false;
        isScoreRecorded = false;
        successTap = 0;
        life = 3;
    }
    /**
     * Get the next MemoTile to be verified.
     */
    public MemoTile getVerifyItems(){
        MemoTile verifyItem = null;
        while (verifyIterator.hasNext()){
            MemoTile item = verifyIterator.next();
            if(item.status == MemoTile.TYPE_ACTIVE){
                verifyItem = item;
                break;
            }
        }
        return verifyItem;
    }

    /**
     * Start the cycle for the MemoTiles to flash.
     */
    public void startCycle(){
        final Iterator<MemoTile> iterator = memoManager.iterator();
        setVerifyIterator();
        setNextToVerify();
        isDisplaying = true;
        view.updateStatus(true);
        Timer timer = new Timer();
        int period = 2000;
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if(iterator.hasNext()){
                    flashMemoTile(iterator.next());
                } else {
                    isDisplaying = false;
                    view.updateStatus(false);
                    this.cancel();
                }
            }
        }, 2000, period);
    }

    /**
     * If the buttons tap are in wrong order, the game ends. Otherwise, flash the button to green.
     *
     * @param pos the position that user taps.
     */
    public void verify(int pos, Context context){
        if (!gameOver && nextToVerify.getId() == pos){
            success(pos);
        } else {
            fail(pos, context);
        }
    }

    /**
     * Continue the the game after a tap the user tapped is correct.
     *
     * @param pos the position that the user tapped
     */
    private void success(int pos){
        view.flashButtonToColor(pos, flashDelay, MemoTile.PRESS_COLOR);
        successTap += 1;
        view.updateScore(successTap);
        nextToVerify = getVerifyItems();
        if (nextToVerify == null) {
            memoManager.updateActiveTiles();
            startCycle();
        }
    }

    /**
     * End the the game after a tap the user tapped is incorrect.
     *
     * @param pos the position that the user tapped
     */
    @SuppressWarnings("unchecked")
    private void fail(int pos, Context context){
        view.flashButtonToColor(pos, flashDelay, MemoTile.WRONG_COLOR);
        life = life == 0 ? 0 : life-1;
        view.updateLife(life);
        gameOver = life == 0;
        if(gameOver && !isScoreRecorded){
            memoManager.setScoreTotal(successTap);
            view.showGameOverDialog(successTap, memoManager.getNewInstance());
            MemoScore score = new MemoScore(memoManager.width,
                    memoManager.isLevel(), memoManager.getScoreTotal());
            scoreManager.saveScore(score, context);
            isScoreRecorded = true;
        }
    }

    /**
     * Flash the tile corresponding to the status of the tile. Green for active tile, red for fake
     * tile.
     *
     * @param tile the MemoTile that need to flash.
     */
    private void flashMemoTile(MemoTile tile){
        if(tile.status == MemoTile.TYPE_ACTIVE){
            view.flashButtonToColor(tile.getId(), flashDelay, MemoTile.ACTIVE_COLOR);
        } else if (tile.status == MemoTile.TYPE_FAKE) {
            view.flashButtonToColor(tile.getId(), flashDelay, MemoTile.FAKE_COLOR);
        }
    }

    @Override
    public void onTapOnTile(Context context, int position) {
        if(!isDisplaying){
            verify(position, context);
        }
    }

    @Override
    public void setMemoManager(MemoManager memoManager) {
        this.memoManager = memoManager;
    }


    /**
     * Set the iterator for verification
     */
    public void setVerifyIterator() {
        verifyIterator = memoManager.iterator();
    }

    /**
     * update next item to verify
     */
    private void setNextToVerify(){
        nextToVerify = getVerifyItems();
    }

    /**
     * number of total success taps of player
     * @return total number of success taps
     */
    @SuppressWarnings("unused")
    public int getSuccessTap() {
        return successTap;
    }

    /**
     * lives player has left
     * @return total lives player has left
     */
    public int getLife() {
        return life;
    }

    @Override
    public void onHintTap() {
        if(!isDisplaying && isAvailableHint){
            view.flashButtonToColor(nextToVerify.getId(), flashDelay, MemoTile.PRESS_COLOR);
            view.deActivateHint();
            isAvailableHint = false;
        }
    }
}
