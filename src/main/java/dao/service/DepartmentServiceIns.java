package dao.service;
import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.dto.Department;
import cn.edu.sustech.cs307.service.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

//TODO：目前这个class的抛出异常工作都没有完成
//TODO:这个class return的值或许需要修改（return null）

public class DepartmentServiceIns implements DepartmentService{
    @Override
    public int addDepartment(String name) {
        String sql = "insert into department values (default,?)";
        try {
            Connection connection = SQLDataSource.getInstance().getSQLConnection();

            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, name);
            preparedStatement.execute();



            connection.close();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return 0;
        //TODO:没说return的是啥,默认为自增id,这里需要进行处理
    }

    @Override
    public void removeDepartment(int departmentId) {

        //TODO:这个departmentid 的值在建表中是一个自增序列，但是实际上可能是每个department有着相对应的departmentid（也就是说这不是一个自增序列，同时建表语句需要进行修改）

        String sql = "delete from department where department.id = ?";
        try {
            Connection connection = SQLDataSource.getInstance().getSQLConnection();

            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setInt(1, departmentId);
            preparedStatement.execute();

            connection.close();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<Department> getAllDepartments() {
        String sql = "select * from department;"; //TODO：需不需要按顺序选择（order by）
        //这个方法是将所有院系的名称(String 类型)放到一个list里面
        List<Department> departmentList = new ArrayList<>();
        //先把所有的数据选出来，然后再一条一条放到list里面去
        ResultSet resultSet ;
        try {
            Connection connection = SQLDataSource.getInstance().getSQLConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            resultSet = preparedStatement.executeQuery();
             while(resultSet.next()){
                 Department departmentListElement = new Department();
                 departmentListElement.id = resultSet.getInt(1);
                 departmentListElement.name = resultSet.getString(2);
                    departmentList.add(departmentListElement);
             }

            connection.close();
            preparedStatement.close();

            return departmentList;


        } catch (SQLException e) {
            e.printStackTrace();
        }
        //TODO：这里默认的return值需不需要进行修改？
        return null;
    }

    @Override
    public Department getDepartment(int departmentId) {
        Department getDepartment = new Department();
        //知道id，得到department的值
        //先执行命令，然后得到resultset的值
        String sql = "select * from department where departmentId = ? ;";
        try {
            Connection connection = SQLDataSource.getInstance().getSQLConnection();

            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.execute();
            int getDepartmentId = preparedStatement.getResultSet().getInt(1);
            String getDepartmentName = preparedStatement.getResultSet().getString(2);
            getDepartment.id = getDepartmentId;
            getDepartment.name = getDepartmentName;

            connection.close();
            preparedStatement.close();

            return getDepartment;


        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }
}
