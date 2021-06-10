package dao.service;

import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.dto.*;
import cn.edu.sustech.cs307.dto.prerequisite.AndPrerequisite;
import cn.edu.sustech.cs307.dto.prerequisite.CoursePrerequisite;
import cn.edu.sustech.cs307.dto.prerequisite.OrPrerequisite;
import cn.edu.sustech.cs307.dto.prerequisite.Prerequisite;
import cn.edu.sustech.cs307.service.*;
import cn.edu.sustech.cs307.exception.*;

import javax.annotation.Nullable;
import java.time.DayOfWeek;
import java.util.*;

import java.sql.*;


public class CourseServiceIns implements CourseService {
//    public static void main(String[] args) {
//        CoursePrerequisite a = new CoursePrerequisite("RD267");
//        CoursePrerequisite b = new CoursePrerequisite("MA102A");
//        CoursePrerequisite c = new CoursePrerequisite("MA102B");
//        List<Prerequisite> orList = new ArrayList<>();
//        orList.add(b);
//        orList.add(c);
//        Prerequisite or = new OrPrerequisite(orList);
//        List<Prerequisite> andList = new ArrayList<>();
//        andList.add(a);
//        andList.add(or);
//        Prerequisite coursePrerequisite = new AndPrerequisite(andList);
//        Queue<List<Prerequisite>> presQueue = new LinkedList<>();
//        Set<List<Prerequisite>> result = new HashSet<>(); // result and group
//        presQueue.add(new ArrayList<>());
//        presQueue.peek().add(coursePrerequisite);
//        parsePre(presQueue, result);
//        for (List<Prerequisite> l : result) {
//            for (Prerequisite p : l) {
//                System.out.print(((CoursePrerequisite) p).courseID + " ");
//            }
//            System.out.println();
//        }
//    }

    public static void parsePre(Queue<List<Prerequisite>> presQueue, Set<List<Prerequisite>> resultSet) {
        List<Prerequisite> listTemp;
        Prerequisite preTemp;
        while (!presQueue.isEmpty()) {
            preTemp = null;
            listTemp = presQueue.poll();
            for (Prerequisite p : listTemp) {
                if (p instanceof AndPrerequisite || p instanceof OrPrerequisite) {
                    preTemp = p;
                    listTemp.remove(p);
                    break;
                }
            }
            if (preTemp == null) {
                resultSet.add(listTemp);
                continue;
            }
            if (preTemp instanceof AndPrerequisite) {
                listTemp.addAll(((AndPrerequisite) preTemp).terms);
                presQueue.offer(listTemp);
            } else {
                for (Prerequisite p : ((OrPrerequisite) preTemp).terms) {
                    List<Prerequisite> newList = new ArrayList<>(listTemp);
                    newList.add(p);
                    presQueue.offer(newList);
                }
            }
        }
    }

    @Override
    public void addCourse(String courseId, String courseName, int credit, int classHour, Course.CourseGrading grading, @Nullable Prerequisite coursePrerequisite) {
        String sql = "insert into course values (default, ?, ?, ?, ?, ?);";
        String relation = "insert into prerequisite values (?, ?, ?);";
        PreparedStatement preparedStatement;
        ResultSet resultSet;


        try {
            Connection connection = SQLDataSource.getInstance().getSQLConnection();

            // add to course table not handling pre
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, courseId);
            preparedStatement.setString(2, courseName);
            preparedStatement.setInt(3, credit);
            preparedStatement.setInt(4, classHour);
            preparedStatement.setBoolean(5, grading == Course.CourseGrading.HUNDRED_MARK_SCORE);
            preparedStatement.execute();

            // return val for this interface
            preparedStatement = connection.prepareStatement("select currval(pg_get_serial_sequence('course', 'course_id'));");

            resultSet = preparedStatement.executeQuery();
            int curVal = 0;
            while (resultSet.next()) {
                curVal = resultSet.getInt(1);
            }

            // parse and add relation prerequisite
            if (coursePrerequisite != null) {
                Queue<List<Prerequisite>> presQueue = new LinkedList<>();
                Set<List<Prerequisite>> result = new HashSet<>(); // result and group
                presQueue.add(new ArrayList<>());
                presQueue.peek().add(coursePrerequisite);
                parsePre(presQueue, result);
                int groupNum = 1, id_serial = 0;
                for (List<Prerequisite> l : result) {
                    for (Prerequisite p : l) {
                        // query id_serial corresponding to courseId
                        preparedStatement = connection.prepareStatement("select course_id from course where id_code = ?;");
                        preparedStatement.setString(1, ((CoursePrerequisite) p).courseID);
                        resultSet = preparedStatement.executeQuery();
                        while (resultSet.next()) {
                            id_serial = resultSet.getInt(1);
                        }

                        // insert record(curVal, id_serial, groupNum) into pre table
                        preparedStatement = connection.prepareStatement(relation);
                        preparedStatement.setInt(1, curVal);
                        preparedStatement.setInt(2, id_serial);
                        preparedStatement.setInt(3, groupNum);
                        preparedStatement.execute();
                    }
                    groupNum++;
                }
            }
            connection.close();
            preparedStatement.close();
        } catch (SQLException e) {
            throw new IntegrityViolationException();
        }
    }

    @Override
    public int addCourseSection(String courseId, int semesterId, String sectionName, int totalCapacity) {
        String courseSection = "insert into section values (default, ?,?,?,?);";
        String courseSemester = "insert into section_semester values (?,?);";
        PreparedStatement preparedStatement;
        ResultSet resultSet;
        try {
            Connection connection = SQLDataSource.getInstance().getSQLConnection();

            // query id_serial according to courseId
            preparedStatement = connection.prepareStatement("select course_id from course where id_code = ?;");
            preparedStatement.setString(1, courseId);
            resultSet = preparedStatement.executeQuery();
            int cid = 0;
            while (resultSet.next()) {
                cid = resultSet.getInt(1);
            }

            // insert record into courseSection table
            preparedStatement = connection.prepareStatement(courseSection);
            preparedStatement.setInt(1, cid);
            preparedStatement.setString(2, sectionName);
            preparedStatement.setInt(3, totalCapacity);
            preparedStatement.setInt(4, totalCapacity);
            preparedStatement.execute();

            // return val for this interface
            preparedStatement = connection.prepareStatement("select currval(pg_get_serial_sequence('section', 'section_id'));");
            resultSet = preparedStatement.executeQuery();
            int curId = 0;
            while (resultSet.next()) {
                curId = resultSet.getInt(1);
            }

            // insert record into section_semester
            preparedStatement = connection.prepareStatement(courseSemester);
            preparedStatement.setInt(1, curId);
            preparedStatement.setInt(2, semesterId);
            preparedStatement.execute();


            // close connection
            connection.close();
            preparedStatement.close();
            return curId;
        } catch (SQLException e) {
            throw new IntegrityViolationException();
        }
    }

    @Override
    public int addCourseSectionClass(int sectionId, int instructorId, DayOfWeek dayOfWeek, Set<Short> weekList, short classStart, short classEnd, String location) {
        String classInsert = "insert into class values (default, ?, ?, ?, ?, ?, ?);";
        String classWeek = "insert into week_class values (?,?);";
        PreparedStatement preparedStatement;
        ResultSet resultSet;
        try {
            Connection connection = SQLDataSource.getInstance().getSQLConnection();

            // insert record into courseSection table
            preparedStatement = connection.prepareStatement(classInsert);
            preparedStatement.setInt(1, sectionId);
            preparedStatement.setInt(2, instructorId);
            preparedStatement.setInt(3, dayOfWeek.ordinal());
            preparedStatement.setInt(4, classStart);
            preparedStatement.setInt(5, classEnd);
            preparedStatement.setString(6, location);
            preparedStatement.execute();

            // return val for this interface
            preparedStatement = connection.prepareStatement("select currval(pg_get_serial_sequence('class', 'class_id'));");
            resultSet = preparedStatement.executeQuery();
            int curId = 0;
            while (resultSet.next()) {
                curId = resultSet.getInt(1);
            }

            // insert record into week_class
            preparedStatement = connection.prepareStatement(classWeek);
            for (Short week : weekList) {
                preparedStatement.setInt(1, week);
                preparedStatement.setInt(2, curId);
                preparedStatement.execute();
            }

            // close connection
            connection.close();
            preparedStatement.close();
            return curId;
        } catch (SQLException e) {
            throw new IntegrityViolationException();
        }
    }

    @Override
    public void removeCourse(String courseId) {
        //courseId represents the id of course. For example, CS307, CS309
        String delCourse = "delete from course where id_code = ?;";
        try {
            Connection connection = SQLDataSource.getInstance().getSQLConnection();

            // execute the removeCourse operation
            PreparedStatement preparedStatement = connection.prepareStatement(delCourse);
            preparedStatement.setString(1, courseId);
            preparedStatement.execute();

            // close connection
            connection.close();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void removeCourseSection(int sectionId) {
        // sectionId: id_serial in table section
        String delSection = "delete from section where section_id = ?;";
        try {
            Connection connection = SQLDataSource.getInstance().getSQLConnection();

            // execute remove section operation
            PreparedStatement preparedStatement = connection.prepareStatement(delSection);
            preparedStatement.setInt(1, sectionId);
            preparedStatement.execute();

            // close connection
            connection.close();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void removeCourseSectionClass(int classId) {
        // classId: id_serial in table class
        String sql = "delete from class where class_id = ?;";
        try {
            Connection connection = SQLDataSource.getInstance().getSQLConnection();

            // execute remove class operation
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setInt(1, classId);
            preparedStatement.execute();

            // close connection
            connection.close();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<Course> getAllCourses() {
        String allCourseInf = "select id_code,name,credit,class_hour,grading from course";

        List<Course> allCourseInformation = new ArrayList<>();

        PreparedStatement preparedStatement;
        try {
            Connection connection = SQLDataSource.getInstance().getSQLConnection();

            // execute sel to get target sections
            preparedStatement = connection.prepareStatement(allCourseInf);

            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {

                Course tempCourse = new Course();
                tempCourse.id = resultSet.getString(1);
                tempCourse.name = resultSet.getString(2);
                tempCourse.credit = resultSet.getInt(3);
                tempCourse.classHour = resultSet.getInt(4);
                if (resultSet.getBoolean(5) == false) { //pass or fail grade
                    tempCourse.grading = Course.CourseGrading.PASS_OR_FAIL;
                } else {
                    tempCourse.grading = Course.CourseGrading.HUNDRED_MARK_SCORE;
                }

                allCourseInformation.add(tempCourse);

            }

            // close connection
            connection.close();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return allCourseInformation;
    }

    @Override
    public List<CourseSection> getCourseSectionsInSemester(String courseId, int semesterId) {
        String sel = "select *\n" +
                "from section_semester ss\n" +
                "         join section s on s.section_id = ss.section_id\n" +
                "         join course c on c.course_id = s.course_id\n" +
                "where s.section_id = ?\n" +
                "  and c.id_code = ?;";
        List<CourseSection> sections = new ArrayList<>();
        PreparedStatement preparedStatement;
        try {
            Connection connection = SQLDataSource.getInstance().getSQLConnection();

            // execute sel to get target sections
            preparedStatement = connection.prepareStatement(sel);
            preparedStatement.setInt(1, semesterId);
            preparedStatement.setString(2, courseId);
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                int id = resultSet.getInt("s.section_id");
                String name = resultSet.getString("s.name");
                int total = resultSet.getInt("total_capacity");
                int left = resultSet.getInt("left_capacity");

                CourseSection section = new CourseSection();
                section.id = id;
                section.name = name;
                section.totalCapacity = total;
                section.leftCapacity = left;

                sections.add(section);
            }

            // close connection
            connection.close();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return sections;
    }

    @Override
    public Course getCourseBySection(int sectionId) {
        Course course = new Course();
        String selCourse = "select course_id from section where section_id = ?;";
        String courseInfo = "select * from course where course_id = ?;";
        ResultSet resultSet;
        PreparedStatement preparedStatement;
        try {
            Connection connection = SQLDataSource.getInstance().getSQLConnection();

            // execute get courseId by sectionId operation
            preparedStatement = connection.prepareStatement(selCourse);
            preparedStatement.setInt(1, sectionId);
            resultSet = preparedStatement.executeQuery();
            int courseId = resultSet.getInt(1);

            // get course info according to queried courseId
            preparedStatement = connection.prepareStatement(courseInfo);
            preparedStatement.setInt(1, courseId);
            resultSet = preparedStatement.executeQuery();
            course.id = resultSet.getString("id");
            course.name = resultSet.getString("name");
            course.credit = resultSet.getInt("credit");
            course.classHour = resultSet.getInt("class_hour");
            course.grading = Course.CourseGrading.values()[resultSet.getBoolean("grading") ? 1 : 0];

            // close connection
            connection.close();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return course;
    }

    @Override
    public List<CourseSectionClass> getCourseSectionClasses(int sectionId) {
        String selClass = "select *\n" +
                "from class c\n" +
                "         join instructor i on c.instructor_id = i.ins_id\n" +
                "where c.section_id = ?;";
        String selWeek = "select week from week_class where class_id = ?;";
        ResultSet resultSet, rsTemp;
        PreparedStatement preparedStatement, preTemp;
        List<CourseSectionClass> classes = new ArrayList<>();
        Set<Short> wkList;
        try {
            Connection connection = SQLDataSource.getInstance().getSQLConnection();

            // get classId by sectionId operation
            preparedStatement = connection.prepareStatement(selClass);
            preparedStatement.setInt(1, sectionId);
            resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                CourseSectionClass csc = new CourseSectionClass();
                csc.id = resultSet.getInt("class_id");
                csc.instructor = new Instructor();
                csc.instructor.id = resultSet.getInt("instructor_id");
                csc.instructor.fullName = resultSet.getString("full_name");
                csc.dayOfWeek = DayOfWeek.of(resultSet.getInt("day_of_week"));
                csc.classBegin = (short) resultSet.getInt("class_begin");
                csc.classEnd = (short) resultSet.getInt("class_end");
                csc.location = resultSet.getString("location");

                // get week
                preTemp = connection.prepareStatement(selWeek);
                preTemp.setInt(1, csc.id);
                rsTemp = preTemp.executeQuery();
                wkList = new HashSet<>();
                while (rsTemp.next()) {
                    wkList.add((short) rsTemp.getInt("week"));
                }

                csc.weekList = wkList;
                classes.add(csc);
            }

            // close connection
            connection.close();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return classes;
    }

    @Override
    public CourseSection getCourseSectionByClass(int classId) {
        String selSection = "select *\n" +
                "from section\n" +
                "where section_id = (\n" +
                "    select c.section_id\n" +
                "    from class c\n" +
                "    where c.class_id = ?);";
        ResultSet resultSet;
        PreparedStatement preparedStatement;
        CourseSection section = new CourseSection();
        try {
            Connection connection = SQLDataSource.getInstance().getSQLConnection();

            // get section by classId
            preparedStatement = connection.prepareStatement(selSection);
            preparedStatement.setInt(1, classId);
            resultSet = preparedStatement.executeQuery();
            section.id = resultSet.getInt("section_id");
            section.name = resultSet.getString("name");
            section.totalCapacity = resultSet.getInt("total_capacity");
            section.leftCapacity = resultSet.getInt("left_capacity");

            // close connection
            connection.close();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return section;
    }


    // TODO: first finish enroll student then complete this method, may need a new table
    @Override
    public List<Student> getEnrolledStudentsInSemester(String courseId, int semesterId) {
        return null;
    }
}
