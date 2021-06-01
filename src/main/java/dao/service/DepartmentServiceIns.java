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

public class DepartmentServiceIns implements DepartmentService {
    @Override
    public int addDepartment(String name) {
        String sql = "insert into department values (default, ?)";
        int curVal = 0;
        try {
            Connection connection = SQLDataSource.getInstance().getSQLConnection();

            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, name);
            preparedStatement.execute();

            preparedStatement = connection.prepareStatement("select currval(pg_get_serial_sequence('department', 'department_id'));");
            preparedStatement.execute();
            curVal = preparedStatement.getResultSet().getInt(1);

            connection.close();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return curVal;
    }

    @Override
    public void removeDepartment(int departmentId) {
        String sql = "delete from department where department_id = ?";
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
        String sql = "select * from department;";
        //这个方法是将所有院系的名称(String 类型)放到一个list里面
        List<Department> departmentList = new ArrayList<>();
        //先把所有的数据选出来，然后再一条一条放到list里面去
        ResultSet resultSet;
        try {
            Connection connection = SQLDataSource.getInstance().getSQLConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            resultSet = preparedStatement.executeQuery();

            // iterate every department
            while (resultSet.next()) {
                Department departmentListElement = new Department();
                departmentListElement.id = resultSet.getInt(1);
                departmentListElement.name = resultSet.getString(2);
                departmentList.add(departmentListElement);
            }

            connection.close();
            preparedStatement.close();

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return departmentList;
    }

    @Override
    public Department getDepartment(int departmentId) {
        Department getDepartment = new Department();
        //知道id，得到department的值
        //先执行命令，然后得到resultset的值
        String sql = "select * from department where department_id = ?;";
        ResultSet resultSet;
        try {
            Connection connection = SQLDataSource.getInstance().getSQLConnection();

            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            resultSet = preparedStatement.executeQuery();
            getDepartment.id = resultSet.getInt(1);
            getDepartment.name = resultSet.getString(2);

            connection.close();
            preparedStatement.close();

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return getDepartment;
    }
}
