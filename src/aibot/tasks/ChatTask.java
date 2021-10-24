package aibot.tasks;

import aibot.*;
import arc.math.*;
import arc.util.*;
import mindustry.core.*;

public class ChatTask extends AITask{
    String chat;
    float typingDelay = 0;
    public ChatTask(AIPlayer player,String chat){
        this(player,chat, Mathf.random(0.1f,0.15f));
    }
    public ChatTask(AIPlayer player,String chat,float typingSpeed){
        super(player, player.pos);
        this.chat=chat;
        typingDelay = chat.length()/typingSpeed;
    }

    @Override
    public void onArrive(){
        player.setChatting(true);

    }

    @Override
    public void doTask(){
        typingDelay-= Time.delta;
        if(typingDelay < 0){
            NetClient.sendChatMessage(player.get(), chat);
            player.setChatting(false);
            this.taskActive=false;
        }
    }

    @Override
    public void interrupt(){
        player.setChatting(false);
    }
}
