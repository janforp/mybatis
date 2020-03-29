package org.apache.ibatis.submitted.result_handler;

import org.apache.ibatis.session.ResultContext;
import org.apache.ibatis.session.ResultHandler;

import java.util.ArrayList;
import java.util.List;

public class UserResultHandler implements ResultHandler {

    private List<User> users;

    public UserResultHandler() {
        super();
        users = new ArrayList<User>();
    }

    @Override
    public void handleResult(ResultContext resultContext) {
        User user = (User) resultContext.getResultObject();
        users.add(user);
    }

    public List<User> getUsers() {
        return users;
    }
}
