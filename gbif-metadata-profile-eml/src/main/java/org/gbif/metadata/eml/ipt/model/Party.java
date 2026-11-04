/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.metadata.eml.ipt.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * One person, organisation or position involved in the resource, together with all the roles it holds.
 *
 * <p>EML lists people per role (creator, contact, metadataProvider, associatedParty), so the same person is repeated
 * for each role. The IPT keeps one list of parties instead ({@link Eml#getParties()}): contact information is entered
 * once and roles are ticked. The per-role lists for writing EML ({@link Eml#getCreators()} etc.) are filtered from
 * it, and EML is read back into it with {@link #merge}.
 *
 * <p>The dataset-level roles are flags rather than role strings, because the associated party role vocabulary has
 * values with the same names (e.g. an associated party with role "metadataProvider" is not a dataset
 * metadataProvider).
 */
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class Party implements Serializable {

  private static final long serialVersionUID = -2390837214560293845L;

  /** Role given to an associated party that has none. */
  public static final String DEFAULT_ASSOCIATED_PARTY_ROLE = "AssociatedParty";

  /** Contact information. Its role is ignored; roles are held by this party. */
  private Agent agent = new Agent();

  /** Listed as eml/dataset/creator. */
  private boolean creator;

  /** Listed as eml/dataset/contact. */
  private boolean contact;

  /** Listed as eml/dataset/metadataProvider. */
  private boolean metadataProvider;

  /** Roles of eml/dataset/associatedParty entries for this party, in order, without duplicates. */
  private List<String> associatedPartyRoles = new ArrayList<>();

  public Party() {}

  public Party(Agent agent) {
    this.agent = agent;
  }

  /**
   * Adds an associated party role, ignoring blanks and duplicates.
   *
   * @param role role to add
   */
  public void addAssociatedPartyRole(String role) {
    if (role != null && !role.isBlank() && !associatedPartyRoles.contains(role)) {
      associatedPartyRoles.add(role);
    }
  }

  /**
   * @return true if this party holds no role at all
   */
  public boolean hasNoRole() {
    return !creator && !contact && !metadataProvider && associatedPartyRoles.isEmpty();
  }

  /**
   * Merges the per-role agent lists into parties. Agents with the same contact information (see
   * {@link Agent#isSameParty(Agent)}) become one party holding all their roles.
   *
   * <p>The order of the parties keeps the order within each role wherever the roles agree, so that
   * {@link Eml#getCreators()} and {@link Eml#getContacts()} give back the same creator (author) order and the same
   * primary contact. Ties are broken by first appearance, looking at creators, then contacts, then metadata
   * providers, then associated parties. Where two roles disagree (A before B as creators, B before A as contacts)
   * the first appearance wins.
   *
   * <p>Associated parties without a role get {@link #DEFAULT_ASSOCIATED_PARTY_ROLE}. The parties hold copies of the
   * agents, with no role.
   *
   * <p>Lossy cases: the same party repeated within one role is collapsed into one entry, and associated parties are
   * grouped by party.
   */
  public static List<Party> merge(
      List<Agent> creators,
      List<Agent> contacts,
      List<Agent> metadataProviders,
      List<Agent> associatedParties) {
    List<Party> parties = new ArrayList<>();
    List<List<Integer>> sequences = new ArrayList<>();

    sequences.add(collect(parties, creators, (p, a) -> p.creator = true));
    sequences.add(collect(parties, contacts, (p, a) -> p.contact = true));
    sequences.add(collect(parties, metadataProviders, (p, a) -> p.metadataProvider = true));
    sequences.add(
        collect(
            parties,
            associatedParties,
            (p, a) -> p.addAssociatedPartyRole(roleOrDefault(a.getRole()))));

    return order(parties, sequences);
  }

  /**
   * @return the associated party role, or {@link #DEFAULT_ASSOCIATED_PARTY_ROLE} if it is blank
   */
  static String roleOrDefault(String role) {
    return StringUtils.isBlank(role) ? DEFAULT_ASSOCIATED_PARTY_ROLE : role;
  }

  /**
   * The eml/dataset/associatedParty entries of the parties: one copy of the agent per associated party role, with
   * that role set. Parties without an agent are skipped.
   */
  static List<Agent> associatedParties(List<Party> parties) {
    List<Agent> associatedParties = new ArrayList<>();
    for (Party party : parties) {
      if (party == null || party.agent == null) {
        continue;
      }
      for (String role : party.associatedPartyRoles) {
        associatedParties.add(withRole(party.agent, role));
      }
    }
    return associatedParties;
  }

  private interface RoleSetter {
    void apply(Party party, Agent agent);
  }

  /**
   * Adds the agents to the parties (finding or creating a party for each) and returns the party indexes in list
   * order, each party once (at its first position).
   */
  private static List<Integer> collect(List<Party> parties, List<Agent> agents, RoleSetter setter) {
    List<Integer> sequence = new ArrayList<>();
    if (agents == null) {
      return sequence;
    }
    for (Agent agent : agents) {
      if (agent == null) {
        continue;
      }
      int index = indexOf(parties, agent);
      if (index < 0) {
        parties.add(new Party(withRole(agent, null)));
        index = parties.size() - 1;
      }
      setter.apply(parties.get(index), agent);
      if (!sequence.contains(index)) {
        sequence.add(index);
      }
    }
    return sequence;
  }

  private static int indexOf(List<Party> parties, Agent agent) {
    for (int i = 0; i < parties.size(); i++) {
      if (parties.get(i).agent.isSameParty(agent)) {
        return i;
      }
    }
    return -1;
  }

  /**
   * Orders the parties so that every sequence keeps its order where possible: a topological sort over "comes
   * before" edges taken from each sequence, picking the earliest first-seen party when several are free. A cycle
   * (sequences disagree) is broken by taking the earliest first-seen remaining party.
   */
  private static List<Party> order(List<Party> parties, List<List<Integer>> sequences) {
    int n = parties.size();
    boolean[][] before = new boolean[n][n];
    int[] incoming = new int[n];
    for (List<Integer> sequence : sequences) {
      for (int i = 1; i < sequence.size(); i++) {
        int from = sequence.get(i - 1);
        int to = sequence.get(i);
        if (!before[from][to]) {
          before[from][to] = true;
          incoming[to]++;
        }
      }
    }

    boolean[] done = new boolean[n];
    List<Party> ordered = new ArrayList<>(n);
    while (ordered.size() < n) {
      int next = -1;
      for (int i = 0; i < n; i++) {
        if (!done[i] && incoming[i] == 0) {
          next = i;
          break;
        }
      }
      if (next < 0) {
        // cycle: roles disagree on the order, first appearance wins
        for (int i = 0; i < n; i++) {
          if (!done[i]) {
            next = i;
            break;
          }
        }
      }
      done[next] = true;
      ordered.add(parties.get(next));
      for (int j = 0; j < n; j++) {
        if (before[next][j]) {
          before[next][j] = false;
          incoming[j]--;
        }
      }
    }
    return ordered;
  }

  private static Agent withRole(Agent agent, String role) {
    Agent copy = agent.copy();
    copy.setRole(role);
    return copy;
  }
}
