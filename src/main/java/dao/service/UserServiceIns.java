package dao.service;
import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.dto.Instructor;
import cn.edu.sustech.cs307.dto.Major;
import cn.edu.sustech.cs307.dto.Student;
import cn.edu.sustech.cs307.dto.User;
import cn.edu.sustech.cs307.service.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UserServiceIns implements UserService{
    @Override
    public void removeUser(int userId) {
        //先查询是否满足条件，然后再进行删除工作
        //先找到用户在什么地方然后进行删除
        String userJudge = "select ins_id from instructor where ins_id = ?;";
        ResultSet resultSet;
        try {
            Connection connection = SQLDataSource.getInstance().getSQLConnection();

            // execute operation
            PreparedStatement preparedStatement = connection.prepareStatement(userJudge);
            preparedStatement.setInt(1, userId);
            resultSet = preparedStatement.executeQuery();
            if(resultSet == null){
                //说明这个userId表示学生
                String deleteStd = "delete from student where std_id = ?";
                preparedStatement = connection.prepareStatement(deleteStd);
                preparedStatement.setInt(1, userId);
                preparedStatement.execute();
            }else{
                //说明这个userId表示老师
                String deleteIns = "delete from instructor where ins_id = ?";
                preparedStatement = connection.prepareStatement(deleteIns);
                preparedStatement.setInt(1, userId);
                preparedStatement.execute();
            }


            // close connection
            connection.close();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    @Override
    public List<User> getAllUsers() {
        String userStd = "select std_id,full_name from student;";
        String userIns = "select ins_id,full_name  from instructor";
        //这个方法是将所有院系的名称(String 类型)放到一个list里面
        List<User> userList = new ArrayList<>();
        //先把所有的数据选出来，然后再一条一条放到list里面去
        ResultSet resultSet;
        try {
            Connection connection = SQLDataSource.getInstance().getSQLConnection();

            PreparedStatement preparedStatement = connection.prepareStatement(userStd);
            resultSet = preparedStatement.executeQuery();
            while(resultSet.next()){
                User tempInsUser = new Instructor();
                tempInsUser.id = resultSet.getInt(1);
                tempInsUser.fullName = resultSet.getString(2);
                userList.add(tempInsUser);
            }

            preparedStatement = connection.prepareStatement(userIns);
            resultSet = preparedStatement.executeQuery();
            while(resultSet.next()){
                User tempStdUser = new Student();
                tempStdUser.id = resultSet.getInt(1);
                tempStdUser.fullName = resultSet.getString(2);
                userList.add(tempStdUser);
            }



            connection.close();
            preparedStatement.close();

            return userList;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return userList;
    }

    @Override
    public User getUser(int userId) {
        String userJudge = "select ins_id from instructor where ins_id = ?;";
        ResultSet resultSet;
        User user;

        try {
            Connection connection = SQLDataSource.getInstance().getSQLConnection();

            // execute operation
            PreparedStatement preparedStatement = connection.prepareStatement(userJudge);
            preparedStatement.setInt(1, userId);
            resultSet = preparedStatement.executeQuery();
            if(resultSet == null){
                //说明这个userId表示学生
                String getStd = "select std_id,full_name from student where std_id = ?";
                preparedStatement = connection.prepareStatement(getStd);
                preparedStatement.setInt(1, userId);
                preparedStatement.executeQuery();
                resultSet = preparedStatement.executeQuery();
                user = new Student();
                user.id = resultSet.getInt(1);
                user.fullName = resultSet.getString(2);
            }else{
                //说明这个userId表示老师
                String getIns = "select ins_id,full_name from instructor where ins_id = ?";
                preparedStatement = connection.prepareStatement(getIns);
                preparedStatement.setInt(1, userId);
                resultSet = preparedStatement.executeQuery();
                user = new Instructor();
                user.id = resultSet.getInt(1);
                user.fullName = resultSet.getString(2);
            }



            // close connection
            connection.close();
            preparedStatement.close();

            return user;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        //TODO:这里的return null还需要吗
        return null;
    }
}
