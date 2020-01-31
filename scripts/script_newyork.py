import json
import datetime
import csv
import re

timestamp_format 	= "%Y-%m-%d %H:%M:%S"
city				= "New York"
city_tag			= "NYC"

with open('../datasets/newyork.csv', newline='') as csvfile:
	reader = csv.DictReader(csvfile)
	count = 0
	for row in reader:
		normalized_row 	= {}
		time 			= {}
		space 			= {}
		rider 			= {}

		# substitute year to make data uniform
		year = 2015 + count % 4
		count += 1
		time["year"] = year
		time["timestamp_start"] = re.sub('201\\d', str(year), row["pickup_datetime"])
		time["timestamp_end"] 	= re.sub('201\\d', str(year), row["dropoff_datetime"])

		space["latitude_start"] = row["pickup_latitude"]
		space["latitude_end"] 	= row["dropoff_latitude"]

		space["longitude_start"] = row["pickup_longitude"]
		space["longitude_end"] 	 = row["dropoff_longitude"]

		if row["gender_id"] == '1':
			rider["gender"] = 'M'
		elif row["gender_id"] == '2':
			rider["gender"] = 'F'

		normalized_row["time"] 	= time
		normalized_row["city"] 	= city
		normalized_row["space"] = space
		normalized_row["rider"] = rider

		print(json.dumps(normalized_row))
