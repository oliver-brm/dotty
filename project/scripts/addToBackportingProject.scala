//> using scala 3.3.1
//> using toolkit 0.2.1
//> using lib pro.kordyjan::pytanie:0.1.6

import pytanie.*
import sttp.client4.*

lazy val apiToken =
  System.getenv("GRAPHQL_API_TOKEN")

case class ID(value: String) derives WrapperVariable

@main def run(number: Int) =
  val (id, date) = getPrData(number)
  val newId = addItem(id)
  timestampItem(newId, date)

def getPrData(number: Int): (ID, String) =
  val res = query"""
    |query getPR {
    |  repository(owner: "lampepfl", name:"dotty") {
    |    pullRequest(number: 17570) {
    |      id
    |      mergedAt
    |    }
    |  }
    |}
    """.send(
      uri"https://api.github.com/graphql",
      "Kordyjan",
      apiToken
    )
  (ID(res.repository.pullRequest.id), res.repository.pullRequest.mergedAt)

def timestampItem(id: ID, date: String) =
  query"""
    |mutation editField {
    |  updateProjectV2ItemFieldValue(input: {
    |    projectId: "PVT_kwDOACj3ec4AWSoi",
    |    itemId: $id,
    |    fieldId: "PVTF_lADOACj3ec4AWSoizgO7uJ4",
    |    value: { text: $date }
    |  }) {
    |    projectV2Item {
    |      updatedAt
    |    }
    |  }
    |}
    """.send(
      uri"https://api.github.com/graphql",
      "Kordyjan",
      apiToken
    )

def addItem(id: ID) =
  val res = query"""
    |mutation addItem {
    |  addProjectV2ItemById(input: {
    |    projectId: "PVT_kwDOACj3ec4AWSoi",
    |    contentId: $id
    |  }) {
    |    item {
    |      id
    |    }
    |  }
    |}
    """.send(
      uri"https://api.github.com/graphql",
      "Kordyjan",
      apiToken
    )
  ID(res.addProjectV2ItemById.item.id)