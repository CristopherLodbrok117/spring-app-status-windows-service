package app.winsw.exception;

public class ChampionNotFoundException extends RuntimeException{
    public  ChampionNotFoundException(long id){
        super("Couldn't find champion with ID: " + id);
    }
}
