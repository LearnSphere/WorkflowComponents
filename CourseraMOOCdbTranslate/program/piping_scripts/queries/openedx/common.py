from ...utilities import db

def GetFullyQualifiedID(doc):
    id = doc["_id"]
    return "{}://{}/{}/{}/{}".format(id['tag'], id['org'], id['course'], id['category'], id['name'])
    
def GetStaffUsers(vars):
    staff_group_id = db.Select(vars['curs']['lms'], "SELECT * FROM auth_group WHERE name='{}'".format('staff_' + vars['source']['course_id']))[0]['id']
    staff_user_group_rows = db.Select(vars['curs']['lms'], "SELECT * FROM auth_user_groups WHERE group_id={}".format(staff_group_id))
    user_id_in_list = [str(r['user_id']) for r in staff_user_group_rows]
    staff_users = db.Select(vars['curs']['lms'], "SELECT auth_user.email AS email, auth_userprofile.country AS country FROM auth_user JOIN auth_userprofile ON auth_user.id=auth_userprofile.user_id WHERE auth_user.id IN ({})".format(",".join(user_id_in_list)))
    return staff_users
    
def GetInstructorUsers(vars):
    inst_group_id = db.Select(vars['curs']['lms'], "SELECT * FROM auth_group WHERE name='{}'".format('instructor_' + vars['source']['course_id']))[0]['id']
    inst_user_group_rows = db.Select(vars['curs']['lms'], "SELECT * FROM auth_user_groups WHERE group_id={}".format(inst_group_id))
    user_id_in_list = [str(r['user_id']) for r in inst_user_group_rows]
    inst_users = db.Select(vars['curs']['lms'], "SELECT auth_user.email AS email, auth_userprofile.country AS country FROM auth_user JOIN auth_userprofile ON auth_user.id=auth_userprofile.user_id WHERE auth_user.id IN ({})".format(",".join(user_id_in_list)))
    return inst_users

def GetStudentUsers(vars):
    student_users = []
    q = "SELECT au.email AS email, aup.country AS country FROM auth_user AS au JOIN auth_userprofile AS aup ON au.id=aup.user_id JOIN student_courseenrollment AS sce ON au.id=sce.user_id WHERE sce.course_id='{}'".format(vars['source']['course_id'])
    if vars['options']['debug']:
        q += " LIMIT 0,{}".format(vars['options']['num_students_debug_mode'])
    student_users = db.Select(vars['curs']['lms'], q)
    return student_users