import uuid

from flask import Flask
from flask import request
from flask import jsonify
import hashlib
import os

from google.api_core.exceptions import GoogleAPICallError
from google.cloud import bigquery
from dotenv import load_dotenv
from uuid import uuid4

app = Flask(__name__)
load_dotenv()  # load .env

os.environ["GOOGLE_APPLICATION_CREDENTIALS"] = os.getenv('GCP_PATH')

client = bigquery.Client()


# TODO: Obscure SQL statements to prevent attacks
@app.route("/users/checkUniqueUsername", methods=['GET'])
def tempMethod():
    email = request.args.get('email')
    username = request.args.get('username')

    retVal = {'foundEmail': 'false', 'foundUsername': 'false', 'badCall': 'false'}

    if email is not None and username is not None:
        query = """
        SELECT email FROM {}
        WHERE email=@email
        """.format(os.getenv('GBQ_USERS'))

        config = bigquery.QueryJobConfig(
            query_parameters=[
                bigquery.ScalarQueryParameter("email", "STRING", email)
            ]
        )

        response = client.query(query, job_config=config)
        responseResult = response.result()

        for row in responseResult:
            retVal['foundEmail'] = 'true'

        query = """
                SELECT username FROM {}
                WHERE username=@username
                """.format(os.getenv('GBQ_USERS'))

        config = bigquery.QueryJobConfig(
            query_parameters=[
                bigquery.ScalarQueryParameter("username", "STRING", username)
            ]
        )

        response = client.query(query, job_config=config)
        responseResult = response.result()

        for row in responseResult:
            retVal['foundUsername'] = 'true'
    else:
        retVal['badCall'] = 'true'

    return retVal


# Methods for User table
# TODO: Ensure unique email addresses
@app.route("/users/createUsers", methods=['POST'])
def createUsers():
    content = request.json

    # Return value from URL or None

    firstName = content.get('firstName')
    lastName = content.get('lastName')
    email = content.get('email')
    username = content.get('username')
    password = content.get('password')

    # Create salt for password, then hash password
    salt = uuid4().bytes
    storeSalt = uuid.UUID(bytes=salt).int # Can be stored in database, equal to salt
    passwordBytes = password.encode('utf-8')
    dk1 = hashlib.pbkdf2_hmac('sha256', passwordBytes, salt, 100000)

    createQuery = """
            INSERT INTO `{}`(firstName, lastName, email, username, password, salt)
            VALUES(@firstName, @lastName, @email, @username, @password, @salt)
            """.format(os.getenv('GBQ_USERS'))

    config = bigquery.QueryJobConfig(
        query_parameters=[
            bigquery.ScalarQueryParameter("firstName", "STRING", firstName),
            bigquery.ScalarQueryParameter("lastName", "STRING", lastName),
            bigquery.ScalarQueryParameter("email", "STRING", email),
            bigquery.ScalarQueryParameter("username", "STRING", username),
            bigquery.ScalarQueryParameter("password", "STRING", dk1.hex()),
            bigquery.ScalarQueryParameter("salt", "STRING", str(storeSalt)),

        ]
    )
    response = client.query(createQuery, job_config=config)
    responseResult = response.result()

    retVal = {"success": "false"}

    if response.num_dml_affected_rows != 0:
        createQuery = """
                        INSERT INTO `{}`(firstname, lastname, email, username, bio, employerrating, workerrating)
                        VALUES(@firstName, @lastName, @email, @username, '', 0, 0)
                        """.format(os.getenv('GBQ_PROFILE'))

        config = bigquery.QueryJobConfig(
            query_parameters=[
                bigquery.ScalarQueryParameter("firstName", "STRING", firstName),
                bigquery.ScalarQueryParameter("lastName", "STRING", lastName),
                bigquery.ScalarQueryParameter("email", "STRING", email),
                bigquery.ScalarQueryParameter("username", "STRING", username)
            ]
        )

        response = client.query(createQuery, job_config=config)
        responseResult = response.result()

        if response.num_dml_affected_rows != 0:
            retVal = {"success": "true"}

    return jsonify(retVal)


@app.route("/users/username", methods=['GET'])
def getUserName():
    email = request.args.get('email')
    query = """
    SELECT username FROM `{}`
    WHERE email=@email
    """.format(os.getenv('GBQ_USERS'))

    config = bigquery.QueryJobConfig(
        query_parameters=[
            bigquery.ScalarQueryParameter("email", "STRING", email)
        ]
    )

    response = client.query(query, job_config=config)
    returnResponse = response.result()

    name = {}
    # This should only return one user. Right now it reassigns userID repeatedly
    for row in returnResponse:
        name["username"] = row.username

    return jsonify(name)


@app.route("/users/delete", methods=['DELETE'])
def deleteUser():
    email = request.args.get('email')

    query = """
    DELETE FROM `{}`
    WHERE email=@email
    """.format(os.getenv('GBQ_USERS'))

    config = bigquery.QueryJobConfig(
        query_parameters=[
            bigquery.ScalarQueryParameter("email", "STRING", email)
        ]
    )

    response = client.query(query, job_config=config)
    returnResponse = response.result()

    success = {}

    if response.num_dml_affected_rows == 0:
        success["success"] = "false"
    else:
        success["success"] = "true"

    return jsonify(success)


@app.route("/users/checkpass", methods=['GET'])
def checkPass():
    password = request.args.get('password')
    email = request.args.get('email')

    query = """
    SELECT password, salt FROM `{}`
    WHERE email=@email
    """.format(os.getenv('GBQ_USERS'))

    config = bigquery.QueryJobConfig(
        query_parameters=[
            bigquery.ScalarQueryParameter("email", "STRING", email)
        ]
    )

    response = client.query(query, job_config=config)
    returnResponse = response.result()

    pass1 = None
    salt = None
    log = "false"

    # This should only return one user. Right now it reassigns userID repeatedly
    for row in returnResponse:
        pass1 = row.password
        salt = row.salt

    if pass1 is not None and salt is not None:
        salt = int(salt)
        salt = uuid.UUID(int=salt, version=4).bytes

        if password is not None:
            # convert passed password to bytes
            passwordBytes = password.encode('utf-8')

            # rehash to check against db
            dk = hashlib.pbkdf2_hmac('sha256', passwordBytes, salt, 100000)

            # convert recomputed hash to string for comparison
            hexCompare = dk.hex().__str__()

            if hexCompare == pass1:
                log = "true"

    success = {"success": log}

    return jsonify(success)


@app.route("/users/updatePassword", methods=['PUT'])
def updatePassword():
    content = request.json
    email = content.get('email')
    newPassword = content.get('password')
    query = """
    UPDATE `{}`
    SET password=@password
    WHERE email=@email
    """.format(os.getenv('GBQ_USERS'), newPassword, email)
    config = bigquery.QueryJobConfig(
        query_parameters=[
            bigquery.ScalarQueryParameter("email", "STRING", email),
            bigquery.ScalarQueryParameter("password", "STRING", newPassword)
        ]
    )

    response = client.query(query, job_config=config)
    retVal = None
    returnResponse = response.result()  # except GoogleAPICallError?

    if response.num_dml_affected_rows == 0:
        retVal = {"success": "false"}
    else:
        retVal = {"success": "true"}

    return jsonify(retVal)


# NEIGHBORS SECTION

@app.route("/neighbors/createNeighbors", methods=['POST'])
def createNeighbors():
    content = request.json
    email1 = content.get('email1')
    email2 = content.get('email2')
    sender = content.get('sender')

    query = """
                INSERT INTO `{}`(email1, email2, status, sender)
                VALUES(@email1, @email2, 0, @sender)
                """.format(os.getenv('GBQ_NEIGHBORS'))

    config = bigquery.QueryJobConfig(
        query_parameters=[
            bigquery.ScalarQueryParameter("email1", "STRING", email1),
            bigquery.ScalarQueryParameter("email2", "STRING", email2),
            bigquery.ScalarQueryParameter("sender", "INT64", sender)
        ]
    )

    response = client.query(query, job_config=config)
    responseResult = response.result()

    if response.num_dml_affected_rows == 0:
        retVal = {"success": "false"}
    else:
        retVal = {"success": "true"}

    return jsonify(retVal)


@app.route("/neighbors/readNeighbors", methods=['GET'])
def readNeighbors():
    email1 = request.args.get('email1')
    email2 = request.args.get('email2')

    query = """
        SELECT status FROM `{}`
        WHERE email1 = @email1 AND email2 = @email2
        """.format(os.getenv('GBQ_NEIGHBORS'))

    config = bigquery.QueryJobConfig(
        query_parameters=[
            bigquery.ScalarQueryParameter("email1", "STRING", email1),
            bigquery.ScalarQueryParameter("email2", "STRING", email2)
        ]
    )

    response = client.query(query, job_config=config)
    returnResponse = response.result()

    retArray = None

    if response.num_dml_affected_rows == 0:
        retArray = {"status": "none"}
    else:
        for row in returnResponse:
            if row.status == 0:
                retArray = {"status": "pending"}
            elif row.status == 1:
                retArray = {"status": "accepted"}
            elif row.status == 2:
                retArray = {"status": "declined"}

    return jsonify(retArray)


@app.route("/neighbors/updateNeighbors", methods=['PUT'])
def updateNeighbors():
    content = request.json
    email1 = content.get('email1')
    email2 = content.get('email2')
    status = content.get('status')
    sender = content.get('sender')

    query = """
        UPDATE `{}`
        SET status=@status,
        sender=@sender
        WHERE email1=@email1 AND email2=@email2
        """.format(os.getenv('GBQ_NEIGHBORS'))
    config = bigquery.QueryJobConfig(
        query_parameters=[
            bigquery.ScalarQueryParameter("email1", "STRING", email1),
            bigquery.ScalarQueryParameter("email2", "STRING", email2),
            bigquery.ScalarQueryParameter("sender", "INT64", sender),
            bigquery.ScalarQueryParameter("status", "INT64", status)
        ]
    )

    response = client.query(query, job_config=config)
    returnResponse = response.result()

    success = None

    if response.num_dml_affected_rows == 0:
        success = {"success": "false"}
    else:
        success = {"success": "true"}

    return jsonify(success)


@app.route("/neighbors/deleteNeighbors", methods=['DELETE'])
def deleteNeighbors():
    email1 = request.args.get('email1')
    email2 = request.args.get('email2')

    query = """
    DELETE FROM `{}`
    WHERE email1=@email1 AND email2=@email2
    """.format(os.getenv('GBQ_NEIGHBORS'))

    config = bigquery.QueryJobConfig(
        query_parameters=[
            bigquery.ScalarQueryParameter("email1", "STRING", email1),
            bigquery.ScalarQueryParameter("email2", "STRING", email2)
        ]
    )

    response = client.query(query, job_config=config)
    returnResponse = response.result()

    success = None

    if response.num_dml_affected_rows == 0:
        success = {"success": "false"}
    else:
        success = {"success": "true"}

    return jsonify(success)


# WORKER SECTION


@app.route("/jobWorkers/createWorker", methods=['POST'])
def createWorker():
    content = request.json
    jobid = content.get('jobid')
    numWorkers = int(content.get('numWorkers'))
    workers = []

    for i in range(numWorkers):
        workers.append(content.get('worker{}'.format(i)))

    query = """
                INSERT INTO `{}`(jobid, email)
                VALUES('{}', '{}')
            """.format(os.getenv('GBQ_JOBWORKERS'), jobid, workers.pop())

    # If the table had more than one worker
    if workers:
        query += ","
        for worker in workers:
            query += """('{}', '{}')""".format(jobid, worker)
            if worker != workers[-1]:
                query += ","

    response = client.query(query)
    returnResponse = response.result()

    success = None

    if response.num_dml_affected_rows == 0:
        success = {"success": "false"}
    else:
        success = {"success": "true"}

    return jsonify(success)


@app.route("/jobWorkers/readWorker", methods=['GET'])
def readWorker():
    jobid = request.args.get('jobid')
    query = """
    SELECT email FROM `{}`
    WHERE jobid=@jobid
    """.format(os.getenv('GBQ_JOBWORKERS'))

    config = bigquery.QueryJobConfig(
        query_parameters=[
            bigquery.ScalarQueryParameter("jobid", "STRING", str(jobid))
        ]
    )

    response = client.query(query, config)
    returnResponse = response.result()

    retArray = {}
    numEmails = 0
    # This should only return one user. Right now it reassigns userID repeatedly

    for row in returnResponse:
        retArray['email{}'.format(numEmails)] = row.email
        query = """
        SELECT username FROM `{}`
        WHERE email=@email
        """.format(os.getenv('GBQ_USERS'))

        config = bigquery.QueryJobConfig(
            query_parameters=[
                bigquery.ScalarQueryParameter("email", "STRING", row.email)
            ]
        )

        response = client.query(query, config)
        returnResponse = response.result()

        for row in returnResponse:
            retArray["worker{}".format(numEmails)] = row.username
            numEmails += 1

    retArray["numworkers"] = numEmails

    return jsonify(retArray)


@app.route("/jobWorkers/deleteWorker", methods=['DELETE'])
def deleteWorker():
    jobid = request.args.get('jobid')
    email = request.args.get('email')

    success = {"success": "false"}

    if jobid is not None and email is not None:
        query = """
            DELETE FROM `{}`
            WHERE email='{}'
            AND jobid='{}'
            """.format(os.getenv('GBQ_JOBWORKERS'), email, jobid)
        response = client.query(query)
        returnResponse = response.result()

        if response.num_dml_affected_rows != 0:
            success = {"success": "true"}

    return jsonify(success)


# PROFILE SECTION


@app.route("/profile/createProfile", methods=['POST'])
def createProfile():
    content = request.json

    email = content.get('email')
    username = content.get('username')
    firstname = content.get('firstname')
    lastname = content.get('lastname')
    bio = content.get('bio')
    employerrating = float(content.get('employerrating'))
    workerrating = float(content.get('workerrating'))
    query = """
    INSERT INTO `{}`(email, username, firstname, lastname, bio, employerrating, workerrating)
            VALUES('{}', '{}', '{}', '{}', '{}', {}, {})
    """.format(os.getenv('GBQ_PROFILE'), email, username, firstname, lastname, bio, employerrating, workerrating)
    response = client.query(query)
    retVal = None
    responseResult = response.result()

    if response.num_dml_affected_rows == 0:
        retVal = {"success": "false"}
    else:
        retVal = {"success": "true"}

    return jsonify(retVal)


@app.route("/profile/readProfile", methods=['GET'])
def readProfile():
    email = request.args.get('email')
    query = """
    SELECT * FROM {}
    WHERE email = @email
    LIMIT 1
    """.format(os.getenv('GBQ_PROFILE'))

    config = bigquery.QueryJobConfig(
        query_parameters=[
            bigquery.ScalarQueryParameter("email", "STRING", email)
        ]
    )

    response = client.query(query, job_config=config)
    retVal = {}
    responseResult = response.result()

    for row in responseResult:
        retVal["username"] = row.username
        retVal["firstname"] = row.firstname
        retVal["lastname"] = row.lastname
        retVal["bio"] = row.bio
        retVal["employerrating"] = row.employerrating
        retVal["workerrating"] = row.workerrating

    return jsonify(retVal)


@app.route("/profile/deleteProfile", methods=['DELETE'])
def deleteProfile():
    email = request.args.get('email')
    query = """
    DELETE FROM {}
    WHERE email=@email
    """.format(os.getenv('GBQ_PROFILE'))

    config = bigquery.QueryJobConfig(
        query_parameters=[
            bigquery.ScalarQueryParameter("email", "STRING", email)
        ]
    )

    response = client.query(query, job_config=config)
    returnResponse = response.result()

    success = None

    if response.num_dml_affected_rows == 0:
        success = {"success": "false"}
    else:
        success = {"success": "true"}

    return jsonify(success)


@app.route("/profile/updateProfile", methods=['PUT'])
def updateProfile():
    content = request.json
    email = content.get('email')
    username = content.get('username')
    firstname = content.get('firstname')
    lastname = content.get('lastname')
    bio = content.get('bio')
    employerrating = float(content.get('employerrating'))
    workerrating = float(content.get('workerrating'))
    query = """
    UPDATE `{}`
    SET username=@username,
    firstname=@firstName,
    lastname=@lastName,
    bio=@bio,
    employerrating=@employerrating,
    workerrating=@workerrating
    WHERE email=@email
    """.format(os.getenv('GBQ_PROFILE'))

    config = bigquery.QueryJobConfig(
        query_parameters=[
            bigquery.ScalarQueryParameter("email", "STRING", email),
            bigquery.ScalarQueryParameter("username", "STRING", username),
            bigquery.ScalarQueryParameter("firstName", "STRING", firstname),
            bigquery.ScalarQueryParameter("lastName", "STRING", lastname),
            bigquery.ScalarQueryParameter("bio", "STRING", bio),
            bigquery.ScalarQueryParameter("employerrating", "FLOAT64", employerrating),
            bigquery.ScalarQueryParameter("workerrating", "FLOAT64", workerrating)
        ]
    )

    response = client.query(query, job_config=config)
    retVal = None
    returnResponse = response.result()  # except GoogleAPICallError?

    if response.num_dml_affected_rows == 0:
        retVal = {"success": "false"}
    else:
        retVal = {"success": "true"}

    return jsonify(retVal)


# JOB SECTION

@app.route("/job/createJobLocation", methods=['POST'])
def createJobLocation():
    jobID = uuid4().int
    content = request.json
    email = content.get('email')
    username = content.get('username')
    description = content.get('description')
    tags = content.get('tags')
    categories = content.get('categories')
    jobName = content.get('jobName')
    reward = content.get('reward')
    long = content.get('longitude')
    lat = content.get('latitude')
    dayOfJob = content.get('dayOfJob')
    monthOfJob = content.get('monthOfJob')
    yearOfJob = content.get('yearOfJob')

    query = """
            INSERT INTO `{}`(email, username, description, tags, categories, jobID, jobName, reward, status, latitude, longitude, dayOfJob, monthOfJob, yearOfJob)
            VALUES(@email, @username, @description, @tags, @categories, @jobID, @jobName, @reward, 0, @lat, @long, @dayOfJob, @monthOfJob, @yearOfJob)
            """.format(os.getenv('GBQ_JOBPOSTS'))
    config = bigquery.QueryJobConfig(
        query_parameters=[
            bigquery.ScalarQueryParameter("email", "STRING", email),
            bigquery.ScalarQueryParameter("username", "STRING", username),
            bigquery.ScalarQueryParameter("description", "STRING", description),
            bigquery.ScalarQueryParameter("tags", "STRING", tags),
            bigquery.ScalarQueryParameter("categories", "STRING", categories),
            bigquery.ScalarQueryParameter("jobID", "STRING", str(jobID)),
            bigquery.ScalarQueryParameter("jobName", "STRING", jobName),
            bigquery.ScalarQueryParameter("reward", "STRING", reward),
            bigquery.ScalarQueryParameter("lat", "FLOAT", lat),
            bigquery.ScalarQueryParameter("long", "FLOAT", long),
            bigquery.ScalarQueryParameter("dayOfJob", "INT64", dayOfJob),
            bigquery.ScalarQueryParameter("monthOfJob", "INT64", monthOfJob),
            bigquery.ScalarQueryParameter("yearOfJob", "INT64", yearOfJob)
        ]
    )

    response = client.query(query, job_config=config)
    responseResult = response.result()

    retVal = {"success": "false"}

    if response.num_dml_affected_rows != 0:
        retVal = {"success": "true"}
        if tags:
            tags = tags.split('#')

            # Filter out any blanks using list comprehension
            tags = [x.replace(" ", "") for x in tags if x]

            if len(tags):

                query = """
                        INSERT INTO `{}`(jobid, tag)
                        VALUES('{}', '{}')
                    """.format(os.getenv('GBQ_JOBTAGS'), jobID, tags.pop())

                # If the table had more than one worker
                if tags:
                    query += ","
                    for tag in tags:
                        query += """('{}', '{}')""".format(jobID, tag)
                        if tag != tags[-1]:
                            query += ","

                response = client.query(query)
                returnResponse = response.result()

    return jsonify(retVal)

# Pushing comment test
# Pushing comment test
# Pushing comment test
@app.route("/job/createJob", methods=['POST'])
def createJob():
    jobID = uuid4().int
    content = request.json
    email = content.get('email')
    username = content.get('username')
    description = content.get('description')
    tags = content.get('tags')
    categories = content.get('categories')
    jobName = content.get('jobName')
    reward = content.get('reward')
    dayOfJob = content.get('dayOfJob')
    monthOfJob = content.get('monthOfJob')
    yearOfJob = content.get('yearOfJob')

    query = """
            INSERT INTO `{}`(email, username, description, tags, categories, jobID, jobName, reward, dayOfJob, monthOfJob, yearOfJob, status)
            VALUES(@email, @username, @description, @tags, @categories, @jobID, @jobName, @reward, @dayOfJob, @monthOfJob, @yearOfJob, 0)
            """.format(os.getenv('GBQ_JOBPOSTS'))
    config = bigquery.QueryJobConfig(
        query_parameters=[
            bigquery.ScalarQueryParameter("email", "STRING", email),
            bigquery.ScalarQueryParameter("username", "STRING", username),
            bigquery.ScalarQueryParameter("description", "STRING", description),
            bigquery.ScalarQueryParameter("tags", "STRING", tags),
            bigquery.ScalarQueryParameter("categories", "STRING", categories),
            bigquery.ScalarQueryParameter("jobID", "STRING", str(jobID)),
            bigquery.ScalarQueryParameter("jobName", "STRING", jobName),
            bigquery.ScalarQueryParameter("reward", "STRING", reward),
            bigquery.ScalarQueryParameter("dayOfJob", "INT64", dayOfJob),
            bigquery.ScalarQueryParameter("monthOfJob", "INT64", monthOfJob),
            bigquery.ScalarQueryParameter("yearOfJob", "INT64", yearOfJob)
        ]
    )

    response = client.query(query, job_config=config)
    responseResult = response.result()

    retVal = {"success": "false"}

    if response.num_dml_affected_rows != 0:
        retVal = {"success": "true"}
        if tags:
            tags = tags.split('#')

            # Filter out any blanks using list comprehension
            tags = [x.replace(" ", "") for x in tags if x]

            if len(tags):

                query = """
                        INSERT INTO `{}`(jobid, tag)
                        VALUES('{}', '{}')
                    """.format(os.getenv('GBQ_JOBTAGS'), jobID, tags.pop())

                # If the table had more than one worker
                if tags:
                    query += ","
                    for tag in tags:
                        query += """('{}', '{}')""".format(jobID, tag)
                        if tag != tags[-1]:
                            query += ","

                response = client.query(query)
                returnResponse = response.result()

    return jsonify(retVal)


@app.route("/job/getCategoryJobs", methods=["GET"])
def getCategoryJobs():

    category = request.args.get('category')

    retVal = []

    numJobs = 0

    if category is not None:
        query = """
                SELECT * FROM {}
                WHERE categories=@category
                """.format(os.getenv('GBQ_JOBPOSTS'))

        config = bigquery.QueryJobConfig(
            query_parameters=[
                bigquery.ScalarQueryParameter("category", "STRING", category)
            ]
        )

        response = client.query(query, job_config=config)
        responseResult = response.result()

        for row in responseResult:
            if row.status == 0:
                tempObj = {}
                tempObj["email"] = row.email
                tempObj["jobid"] = row.jobID
                tempObj["jobTitle"] = row.jobName
                tempObj["username"] = row.username
                tempObj["description"] = row.description
                tempObj["reward"] = row.reward
                tempObj["tags"] = row.tags
                tempObj["category"] = row.categories
                tempObj["status"] = row.status
                tempObj["latitude"] = row.latitude
                tempObj["longitude"] = row.longitude
                tempObj["day"] = row.dayOfJob
                tempObj["month"] = row.monthOfJob
                tempObj["year"] = row.yearOfJob
                retVal.append(tempObj)
                numJobs += 1

    retVal.insert(0, {"numJobs": numJobs})

    return jsonify(retVal)


@app.route("/job/getCategoryLocation", methods=["GET"])
def getCategoryLocation():
    category = request.args.get('category')
    currentLat = request.args.get('latitude')
    currentLong = request.args.get('longitude')
    jobRange = request.args.get('range')

    retVal = []

    numJobs = 0

    if category is not None:
        query = """
                SELECT * FROM {}
                WHERE categories=@category
                AND latitude<(@currentLat + @jobRange)
                AND latitude>(@currentLat - @jobRange)
                AND longitude<(@currentLong + @jobRange)
                AND longitude>(@currentLong - @jobRange)
                """.format(os.getenv('GBQ_JOBPOSTS'))

        config = bigquery.QueryJobConfig(
            query_parameters=[
                bigquery.ScalarQueryParameter("category", "STRING", category),
                bigquery.ScalarQueryParameter("currentLat", "FLOAT", currentLat),
                bigquery.ScalarQueryParameter("currentLong", "FLOAT", currentLong),
                bigquery.ScalarQueryParameter("jobRange", "FLOAT", jobRange)
            ]
        )

        response = client.query(query, job_config=config)
        responseResult = response.result()

        for row in responseResult:
            if row.status == 0:
                tempObj = {}
                tempObj["email"] = row.email
                tempObj["jobid"] = row.jobID
                tempObj["jobTitle"] = row.jobName
                tempObj["username"] = row.username
                tempObj["description"] = row.description
                tempObj["reward"] = row.reward
                tempObj["tags"] = row.tags
                tempObj["category"] = row.categories
                tempObj["status"] = row.status
                tempObj["latitude"] = row.latitude
                tempObj["longitude"] = row.longitude
                tempObj["day"] = row.dayOfJob
                tempObj["month"] = row.monthOfJob
                tempObj["year"] = row.yearOfJob
                retVal.append(tempObj)
                numJobs += 1

    retVal.insert(0, {"numJobs": numJobs})

    return jsonify(retVal)


@app.route("/job/getTagsJobs", methods=['GET'])
def getTagsJobs():
    tags = request.args.get('tags')
    retVal = []

    if tags == "":
        retVal = readLocationJobs()
    else:
        numJobs = 0

        if tags is not None:
            tags = tags.split('#')
            # Filter out any blanks using list comprehension
            tags = [x.replace(" ", "") for x in tags if x]

            if len(tags):
                # Make dictionary (unique) jobs found matching tag
                returnedJobs = {}
                for tag in tags:
                    query = """
                            SELECT jobID, tag FROM {}
                            WHERE tag=@tag
                            """.format(os.getenv('GBQ_JOBTAGS'))

                    config = bigquery.QueryJobConfig(
                        query_parameters=[
                            bigquery.ScalarQueryParameter("tag", "STRING", tag)
                        ]
                    )

                    response = client.query(query, job_config=config)
                    responseResult = response.result()

                    for row in responseResult:
                        # This effectively removes duplicates using dict
                        returnedJobs[row.jobID] = row.tag

                # If any jobs are found, find the job information and return it
                if returnedJobs:
                    for key in returnedJobs:
                        query = """
                            SELECT * FROM {}
                            WHERE jobID=@job
                            """.format(os.getenv('GBQ_JOBPOSTS'))

                        config = bigquery.QueryJobConfig(
                            query_parameters=[
                                bigquery.ScalarQueryParameter("job", "STRING", key)
                            ]
                        )

                        response1 = client.query(query, job_config=config)
                        responseResult1 = response1.result()

                        for row in responseResult1:
                            if row.status == 0:
                                tempObj = {}
                                tempObj["email"] = row.email
                                tempObj["jobid"] = row.jobID
                                tempObj["jobTitle"] = row.jobName
                                tempObj["username"] = row.username
                                tempObj["description"] = row.description
                                tempObj["reward"] = row.reward
                                tempObj["tags"] = row.tags
                                tempObj["category"] = row.categories
                                tempObj["status"] = row.status
                                tempObj["latitude"] = row.latitude
                                tempObj["longitude"] = row.longitude
                                tempObj["day"] = row.dayOfJob
                                tempObj["month"] = row.monthOfJob
                                tempObj["year"] = row.yearOfJob
                                retVal.append(tempObj)
                                numJobs += 1

        # Insert the number of jobs found in the first spot of dict
        retVal.insert(0, {"numJobs": numJobs})

    try:
        retVal = jsonify(retVal)
    except:
        print('Already converted')

    return retVal


@app.route("/job/getTagsLocation", methods=['GET'])
def getTagsLocation():
    tags = request.args.get('tags')
    currentLat = request.args.get('latitude')
    currentLong = request.args.get('longitude')
    jobRange = request.args.get('range')
    retVal = []

    if tags == "":
        retVal = readLocationJobs()
    else:
        numJobs = 0
        if tags is not None:
            tags = tags.split('#')
            # Filter out any blanks using list comprehension
            tags = [x.replace(" ", "") for x in tags if x]

            if len(tags):
                # Make dictionary (unique) jobs found matching tag
                returnedJobs = {}
                for tag in tags:
                    query = """
                            SELECT jobID, tag FROM {}
                            WHERE tag=@tag
                            """.format(os.getenv('GBQ_JOBTAGS'))

                    config = bigquery.QueryJobConfig(
                        query_parameters=[
                            bigquery.ScalarQueryParameter("tag", "STRING", tag)
                        ]
                    )

                    response = client.query(query, job_config=config)
                    responseResult = response.result()

                    for row in responseResult:
                        # This effectively removes duplicates using dict
                        returnedJobs[row.jobID] = row.tag

                # If any jobs are found, find the job information and return it
                if returnedJobs:
                    for key in returnedJobs:
                        query = """
                            SELECT * FROM {}
                            WHERE jobID=@job
                            AND latitude<(@currentLat + @jobRange)
                            AND latitude>(@currentLat - @jobRange)
                            AND longitude<(@currentLong + @jobRange)
                            AND longitude>(@currentLong - @jobRange)
                            """.format(os.getenv('GBQ_JOBPOSTS'))

                        config = bigquery.QueryJobConfig(
                            query_parameters=[
                                bigquery.ScalarQueryParameter("job", "STRING", key),
                                bigquery.ScalarQueryParameter("currentLat", "FLOAT", currentLat),
                                bigquery.ScalarQueryParameter("currentLong", "FLOAT", currentLong),
                                bigquery.ScalarQueryParameter("jobRange", "FLOAT", jobRange)
                            ]
                        )

                        response1 = client.query(query, job_config=config)
                        responseResult1 = response1.result()

                        for row in responseResult1:
                            if row.status == 0:
                                tempObj = {}
                                tempObj["email"] = row.email
                                tempObj["jobid"] = row.jobID
                                tempObj["jobTitle"] = row.jobName
                                tempObj["username"] = row.username
                                tempObj["description"] = row.description
                                tempObj["reward"] = row.reward
                                tempObj["tags"] = row.tags
                                tempObj["category"] = row.categories
                                tempObj["status"] = row.status
                                tempObj["latitude"] = row.latitude
                                tempObj["longitude"] = row.longitude
                                tempObj["day"] = row.dayOfJob
                                tempObj["month"] = row.monthOfJob
                                tempObj["year"] = row.yearOfJob
                                retVal.append(tempObj)
                                numJobs += 1

        # Insert the number of jobs found in the first spot of dict
        retVal.insert(0, {"numJobs": numJobs})

    try:
        retVal = jsonify(retVal)
    except:
        print('Already converted')

    return retVal


@app.route("/job/readJobs", methods=['GET'])
def readJobs():
    query = """
        SELECT * FROM {}
        """.format(os.getenv('GBQ_JOBPOSTS'))

    response = client.query(query)
    responseResult = response.result()

    retVal = []

    numJobs = 0

    for row in responseResult:
        if row.status == 0:
            tempObj = {}
            tempObj["email"] = row.email
            tempObj["jobid"] = row.jobID
            tempObj["jobTitle"] = row.jobName
            tempObj["username"] = row.username
            tempObj["description"] = row.description
            tempObj["reward"] = row.reward
            tempObj["tags"] = row.tags
            tempObj["category"] = row.categories
            tempObj["status"] = row.status
            tempObj["latitude"] = row.latitude
            tempObj["longitude"] = row.longitude
            tempObj["day"] = row.dayOfJob
            tempObj["month"] = row.monthOfJob
            tempObj["year"] = row.yearOfJob
            retVal.append(tempObj)
            numJobs += 1

    retVal.insert(0, {"numJobs": numJobs})

    return jsonify(retVal)


@app.route("/job/readLocationJobs", methods=['GET'])
def readLocationJobs():
    currentLat = request.args.get('latitude')
    currentLong = request.args.get('longitude')
    jobRange = request.args.get('range')

    numJobs = 0
    retVal = []

    if currentLat is not None and currentLong is not None and jobRange is not None:
        query = """
            SELECT * FROM {}
            WHERE latitude<(@currentLat + @jobRange)
            AND latitude>(@currentLat - @jobRange)
            AND longitude<(@currentLong + @jobRange)
            AND longitude>(@currentLong - @jobRange)
            """.format(os.getenv('GBQ_JOBPOSTS'))

        config = bigquery.QueryJobConfig(
            query_parameters=[
                bigquery.ScalarQueryParameter("currentLat", "FLOAT", currentLat),
                bigquery.ScalarQueryParameter("currentLong", "FLOAT", currentLong),
                bigquery.ScalarQueryParameter("jobRange", "FLOAT", jobRange)
            ]
        )

        response = client.query(query, job_config=config)
        responseResult = response.result()

        for row in responseResult:
            if row.status == 0:
                tempObj = {}
                tempObj["email"] = row.email
                tempObj["jobid"] = row.jobID
                tempObj["jobTitle"] = row.jobName
                tempObj["username"] = row.username
                tempObj["description"] = row.description
                tempObj["reward"] = row.reward
                tempObj["tags"] = row.tags
                tempObj["category"] = row.categories
                tempObj["status"] = row.status
                tempObj["latitude"] = row.latitude
                tempObj["longitude"] = row.longitude
                tempObj["day"] = row.dayOfJob
                tempObj["month"] = row.monthOfJob
                tempObj["year"] = row.yearOfJob
                retVal.append(tempObj)
                numJobs += 1

    retVal.insert(0, {"numJobs": numJobs})

    return jsonify(retVal)


@app.route("/job/readJob", methods=['GET'])
def readJob():
    jobID = request.args.get('jobid')

    query = """
        SELECT * FROM {}
        WHERE jobID = @jobID
        """.format(os.getenv('GBQ_JOBPOSTS'))

    config = bigquery.QueryJobConfig(
        query_parameters=[
            bigquery.ScalarQueryParameter("jobID", "STRING", str(jobID))
        ]
    )

    response = client.query(query, job_config=config)
    responseResult = response.result()

    retVal = {}

    for row in responseResult:
        retVal["jobid"] = row.jobID
        retVal["email"] = row.email
        retVal["username"] = row.username
        retVal["description"] = row.description
        retVal["tags"] = row.tags
        retVal["category"] = row.categories

    return jsonify(retVal)


# Return job status (-1 - 2) and if email associated with job worker in that job
@app.route("/job/checkStatus", methods=['GET'])
def checkStatus():
    email = request.args.get('email')
    jobid = request.args.get('jobid')

    retVal = {"status": None, "worker": "false"}

    if email is not None and jobid is not None:
        query = """
        SELECT status FROM {}
        WHERE jobID = @jobid
        """.format(os.getenv('GBQ_JOBPOSTS'))

        config = bigquery.QueryJobConfig(
            query_parameters=[
                bigquery.ScalarQueryParameter("jobid", "STRING", str(jobid))
            ]
        )

        response = client.query(query, job_config=config)
        responseResult = response.result()

        for row in responseResult:
            retVal['status'] = row.status

        # Job found
        if retVal['status'] is not None:
            query = """
            SELECT email FROM {}
            WHERE email=@email
            AND jobid=@jobid
            """.format(os.getenv('GBQ_JOBWORKERS'))
            config = bigquery.QueryJobConfig(
                query_parameters=[
                    bigquery.ScalarQueryParameter("email", "STRING", email),
                    bigquery.ScalarQueryParameter("jobid", "STRING", str(jobid))
                ]
            )

            confirmEmail = None
            response = client.query(query, job_config=config)
            responseResult = response.result()

            for row in responseResult:
                confirmEmail = row.email

            if confirmEmail is not None:
                retVal['worker'] = 'true'
        else:
            retVal['status'] = -1

    return jsonify(retVal)


@app.route('/job/getUserJobs', methods=['GET'])
def getUserJobs():
    email = request.args.get('email')

    retVal = []

    numJobs = 0

    if email is not None:
        query = """
        SELECT jobid FROM {}
        WHERE email=@email
        """.format(os.getenv('GBQ_JOBWORKERS'))
        config = bigquery.QueryJobConfig(
            query_parameters=[
                bigquery.ScalarQueryParameter("email", "STRING", email)
            ]
        )

        response = client.query(query, job_config=config)
        responseResult = response.result()

        userJobs = []

        for row in responseResult:
            userJobs.append(row.jobid)

        for id in userJobs:
            query = """SELECT * FROM {}
            WHERE jobID=@jobid
            """.format(os.getenv('GBQ_JOBPOSTS'))
            config = bigquery.QueryJobConfig(
                query_parameters=[
                    bigquery.ScalarQueryParameter("jobid", "STRING", str(id))
                ]
            )

            response = client.query(query, job_config=config)
            responseResult = response.result()

            for row in responseResult:
                tempObj = {}
                tempObj["jobid"] = row.jobID
                tempObj["email"] = row.email
                tempObj["jobTitle"] = row.jobName
                tempObj["username"] = row.username
                tempObj["description"] = row.description
                tempObj["reward"] = row.reward
                tempObj["tags"] = row.tags
                tempObj["category"] = row.categories
                tempObj["status"] = row.status
                tempObj["day"] = row.dayOfJob
                tempObj["month"] = row.monthOfJob
                tempObj["year"] = row.yearOfJob
                retVal.append(tempObj)
                numJobs += 1

    retVal.insert(0, {"numJobs": numJobs})

    return jsonify(retVal)


@app.route('/job/getEmployerJobs', methods=['GET'])
def getEmployerJobs():
    email = request.args.get('email')

    retVal = []

    numJobs = 0

    if email is not None:
        query = """
        SELECT * FROM {}
        WHERE email=@email
        """.format(os.getenv('GBQ_JOBPOSTS'))
        config = bigquery.QueryJobConfig(
            query_parameters=[
                bigquery.ScalarQueryParameter("email", "STRING", email)
            ]
        )

        response = client.query(query, job_config=config)
        responseResult = response.result()

        for row in responseResult:
            tempObj = {}
            tempObj["jobid"] = row.jobID
            tempObj["email"] = row.email
            tempObj["jobTitle"] = row.jobName
            tempObj["username"] = row.username
            tempObj["description"] = row.description
            tempObj["reward"] = row.reward
            tempObj["tags"] = row.tags
            tempObj["category"] = row.categories
            tempObj["status"] = row.status
            tempObj["day"] = row.dayOfJob
            tempObj["month"] = row.monthOfJob
            tempObj["year"] = row.yearOfJob
            retVal.append(tempObj)
            numJobs += 1

    retVal.insert(0, {"numJobs": numJobs})

    return jsonify(retVal)


@app.route('/job/changeStatus', methods=['PUT'])
def changeStatus():
    content = request.json
    email = content.get('email')
    jobid = content.get('jobid')
    status = content.get('status')

    jobstatus = {"success": "true"}

    if jobid is not None and email is not None:
        query = """
        UPDATE {}
        SET status=@status
        WHERE jobid=@jobid
        AND email=@email
        """.format(os.getenv('GBQ_JOBPOSTS'))

        config = bigquery.QueryJobConfig(
            query_parameters=[
                bigquery.ScalarQueryParameter("email", "STRING", email),
                bigquery.ScalarQueryParameter("jobid", "STRING", str(jobid)),
                bigquery.ScalarQueryParameter("status", "INTEGER", status)
            ]
        )

        response = client.query(query, job_config=config)
        responseResult = response.result()

        if response.num_dml_affected_rows != 0:
            retVal = {"success": "true"}

    return jobstatus


@app.route("/job/acceptJob", methods=['POST'])
def acceptJob():
    content = request.json
    email = content.get('email')
    jobid = content.get('jobid')

    jobstatus = {"success": "false"}

    if jobid is not None and email is not None:
        query = """
        INSERT INTO {} (jobid, email)
        VALUES (@jobid, @email)
        """.format(os.getenv('GBQ_JOBWORKERS'))

        config = bigquery.QueryJobConfig(
            query_parameters=[
                bigquery.ScalarQueryParameter("jobid", "STRING", str(jobid)),
                bigquery.ScalarQueryParameter("email", "STRING", email)
            ]
        )

        response = client.query(query, job_config=config)
        responseResult = response.result()

        if response.num_dml_affected_rows != 0:
            jobstatus = {"success": "true"}

    return jsonify(jobstatus)

@app.route("/job/updateJob", methods=['PUT'])
def updateJob():
    content = request.json
    email = content.get('email')
    jobid = content.get('jobid')
    description = content.get('description')
    tags = content.get('tags')
    categories = content.get('categories')
    query = """
    UPDATE `{}`
    SET description=@description,
    tags=@tags,
    categories=@categories
    WHERE email=@email
    AND jobID=@jobID
    """.format(os.getenv('GBQ_JOBPOSTS'))

    config = bigquery.QueryJobConfig(
        query_parameters=[
            bigquery.ScalarQueryParameter("description", "STRING", description),
            bigquery.ScalarQueryParameter("tags", "STRING", tags),
            bigquery.ScalarQueryParameter("categories", "STRING", categories),
            bigquery.ScalarQueryParameter("email", "STRING", email),
            bigquery.ScalarQueryParameter("jobID", "STRING", str(jobid))
        ]
    )

    response = client.query(query, job_config=config)
    retVal = None
    returnResponse = response.result()  # except GoogleAPICallError?

    if response.num_dml_affected_rows == 0:
        retVal = {"success": "false"}
    else:
        retVal = {"success": "true"}

    return jsonify(retVal)


@app.route("/job/deleteJob", methods=['DELETE'])
def deleteJob():
    jobid = request.args.get('jobid')
    email = request.args.get('email')

    success = {"success": "false"}

    if jobid is not None and email is not None:
        query = """
        DELETE FROM {}
        WHERE email=@email
        AND jobID=@jobID
        """.format(os.getenv('GBQ_JOBPOSTS'))

        config = bigquery.QueryJobConfig(
            query_parameters=[
                bigquery.ScalarQueryParameter("email", "STRING", email),
                bigquery.ScalarQueryParameter("jobID", "STRING", str(jobid))
            ]
        )

        response = client.query(query, job_config=config)
        returnResponse = response.result()

        success = {"success": "true"}

        query = """
                DELETE FROM {}
                WHERE jobID=@jobID
                """.format(os.getenv('GBQ_JOBWORKERS'))

        config = bigquery.QueryJobConfig(
            query_parameters=[
                bigquery.ScalarQueryParameter("jobID", "STRING", str(jobid))
            ]
        )

        response = client.query(query, job_config=config)
        returnResponse = response.result()

    return jsonify(success)


if __name__ == "__main__":
    app.run()
