package ru.mts.ip.workflow.engine.lang.plant;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.google.common.base.Objects;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import ru.mts.ip.workflow.engine.Const;
import ru.mts.ip.workflow.engine.lang.plant.WorkflowExpression.Activity;

@EqualsAndHashCode(of = "id")
public class UmlPath {
  private Set<UmlPath> next = new HashSet<>();
  @Getter
  private String id;
  @Getter
  private String headImpl;
  private Traverser traverser;
  private UmlPath repeatParent;
  private UmlPath breakIf;

  UmlPath(String root, Traverser traverser, List<String> seq) {
    this.traverser = traverser;
    id = root;
    if (id != null && !seq.contains(id)) {
      var suSeq = new ArrayList<>(seq);
      suSeq.add(id);
      Set<String> posibleNext = findPosible(id);
      posibleNext.forEach(n -> {
        var p = new UmlPath(n, traverser, suSeq);
        next.add(p);
      });
    }
    if (isRepeat()) {
      traverser.cicled.add(this);
      Set<UmlPath> ifPaths = find(p -> p.isIf());
      if (ifPaths.size() == 1) {
        breakIf = ifPaths.stream().findFirst().orElseThrow();
      } else {
        for (UmlPath ifPath : ifPaths) {
          if (this.equals(ifPath)) {
            breakIf = ifPath;
            break;
          } else {
            String exit = findExit(ifPath.allFlows(), new ArrayList<>(List.of(ifPath.id)));
            if (anyContains(ifPath, id, exit) && !insideLoop(ifPath)) {
              breakIf = ifPath;
            }
          }
        }
      }
      if (breakIf == null) {
        throw new IllegalStateException();
      } else {
        breakIf.repeatParent = this;
      }
    }
  }

  private boolean insideLoop(UmlPath path) {
    List<List<String>> res = path.allFlows().stream().filter(l -> findCircle(l) < 0)
        .filter(l -> !containsAnyCicle(l)).toList();
    return res.isEmpty();
  }

  private LinkedHashSet<UmlPath> find(Predicate<UmlPath> predicate) {
    LinkedHashSet<UmlPath> res = new LinkedHashSet<>();
    if (predicate.test(this)) {
      res.add(this);
    }
    for (UmlPath p : next) {
      Set<UmlPath> found = p.find(predicate);
      if (found != null) {
        res.addAll(found);
      }
    }
    return res;
  }

  public boolean isParentOfRepeat() {
    return repeatParent != null;
  }

  public Activity asState() {
    return traverser.findState(id);
  }

  public boolean isRepeat() {
    boolean res = false;
    for (List<String> f : allFlows()) {
      res |= findCircle(f) == 0;
    }
    return res;
  }

  public boolean isActivity() {
    return id != null
        && List.of(Const.ActivityType.WORKFLOW_CALL, Const.ActivityType.INJECT, Const.ActivityType.WORKFLOW_CALL, Const.ActivityType.TIMER)
          .contains(traverser.findState(id).getType());
  }

  public boolean isIf() {

    return id != null && traverser.findState(id).getType().equals(Const.ActivityType.SWITCH);
  }


  private boolean containsAnyCicle(List<String> src) {
    for (UmlPath ciclePath : traverser.cicled) {
      if (src.contains(ciclePath.id)) {
        return true;
      }
    }
    return false;
  }

  private String findExit(List<List<String>> flows, List<String> ignore) {
    if (!flows.isEmpty()) {
      List<List<String>> sortes = flows.stream().sorted((l, v) -> v.size() - l.size()).toList();

      if (!insideLoop(this)) {
        sortes = sortes.stream().filter(l -> findCircle(l) < 0).filter(l -> !containsAnyCicle(l))
            .toList();
      }

      List<String> f = sortes.stream().findFirst().orElse(List.of());
      for (String c : f) {
        if (!ignore.contains(c)) {
          boolean all = true;
          for (List<String> seq : sortes) {
            all &= seq.contains(c);
          }
          if (all) {
            return c;
          }
        }
      }
    }
    return null;
  }

  private UmlPath findPath(String id) {
    if (Objects.equal(id, this.id)) {
      return this;
    } else {
      for (UmlPath p : next) {
        UmlPath found = p.findPath(id);
        if (found != null) {
          return found;
        }
      }
    }
    return null;
  }

  private UmlPath findExit() {
    List<List<String>> allFlows = allFlows();
    if (isRepeat()) {
      List<List<String>> notCicled = allFlows.stream().filter(l -> findCircle(l) < 0).toList();

      if (notCicled.size() == 1) {
        String firstComplex = null;
        List<String> seq = notCicled.get(0);
        for (String s : seq) {
          UmlPath path = findPath(s);
          if (path != null && path.isIf()) {
            firstComplex = path.id;
            break;
          }
        }
        List<String> tail = seq.subList(seq.indexOf(firstComplex) + 1, seq.size());
        if (!tail.isEmpty()) {
          return findPath(tail.get(0));
        } else {
          return null;
        }
      } else {
        if (breakIf == this) {
          return null;
        }
        return breakIf.findExit();
      }

    } else if (isIf()) {
      String exitId = findExit(allFlows, new ArrayList<>(List.of(id)));
      if (exitId != null) {
        return findPath(exitId);
      }
    }
    return null;
  }

  private int findCircle(List<String> seq) {
    int i = 0;
    List<String> example = new ArrayList<>(seq);
    Iterator<String> it = example.iterator();
    while (it.hasNext()) {
      String toFind = it.next();
      it.remove();
      if (example.contains(toFind)) {
        return i;
      }
      i++;
    }
    return -1;
  }

  private void allFlows(List<List<String>> allPaths, List<String> path) {
    for (UmlPath p : next) {
      List<String> newPath = new ArrayList<>(path);
      newPath.add(id);
      if (p.next.isEmpty()) {
        newPath.add(p.id);
        allPaths.add(newPath);
      } else {
        p.allFlows(allPaths, newPath);
      }
    }
  }

  private List<List<String>> allFlows() {
    List<List<String>> all = new ArrayList<>();
    allFlows(all, new ArrayList<>());
    return all;
  }


  private UmlPath getPlainNext() {
    if (isRepeat() || isIf()) {
      return findExit();
    }
    return next.stream().findFirst().orElse(null);
  }

  public List<UmlPath> asPlain() {
    List<UmlPath> res = new ArrayList<>();
    res.add(this);
    UmlPath curr = this;
    while ((curr = curr.getPlainNext()) != null) {
      res.add(curr);
    }
    return res;
  }

  private UmlPath next() {
    if (isIf()) {
      return findExit();
    } else {
      return next.stream().findFirst().orElse(null);
    }
  }

  public List<UmlPath> findDetailed(String headName) {
    return detailed().stream().filter(l -> !l.isEmpty())
        .filter(l -> headName.equals(l.stream().findFirst().orElseThrow().getId())).findFirst()
        .orElse(List.of());
  }

  List<List<UmlPath>> detailForRepeat() {
    List<UmlPath> res = new ArrayList<>();
    UmlPath curr = this;
    do {
      if (anyContains(curr.asPlain(), id)) {
        res.add(curr);
      } else {
        break;
      }
    } while ((curr = curr.next()) != null);
    return List.of(res);
  }

  boolean anyContains(Collection<UmlPath> src, String toFind) {
    for (UmlPath p : src) {
      for (List<String> fl : p.allFlows()) {
        if (fl.contains(toFind)) {
          return true;
        }
      }
    }
    return false;
  }

  boolean anyContains(UmlPath src, String toFind, String end) {
    for (List<String> fl : src.allFlows()) {
      for (String el : fl) {
        if (Objects.equal(el, end)) {
          break;
        }
        if (Objects.equal(el, toFind)) {
          return true;
        }
      }
    }
    return false;
  }

  public List<List<UmlPath>> detailed() {
    if (isIf()) {
      List<List<UmlPath>> res = new ArrayList<>();
      List<List<String>> all = allFlows();
      String exit = null;
      List<String> toSkip = new ArrayList<>();

      if (isRepeat()) {
        List<List<String>> notCicled = all.stream().filter(l -> findCircle(l) < 0).toList();
        exit = findExit(notCicled, new ArrayList<>(List.of(id)));
      } else if (repeatParent != null) {
        List<List<String>> notCicled =
            all.stream().filter(l -> !l.contains(repeatParent.id)).toList();
        if (!notCicled.isEmpty()) {
          toSkip.add(notCicled.get(0).get(1));
        }
        res.add(new ArrayList<>());
      } else {
        exit = findExit(all, new ArrayList<>(List.of(id)));
      }

      for (UmlPath p : next) {
        List<UmlPath> plain = p.asPlain();
        if (!plain.isEmpty()) {
          if (toSkip.contains(plain.get(0).getId())) {
            continue;
          }
          if (plain.get(plain.size() - 1) != null) {
            UmlPath last = plain.stream().skip(plain.size() - 1).findFirst().orElseThrow();
            if ((traverser.cicled.contains(last) || Objects.equal(exit, last.id)) && exit == null) {
              plain = plain.subList(0, plain.size() - 1);
            } else if (plain.size() > 1) {
              plain = plain.subList(0, plain.indexOf(findPath(exit)));
            }
          }
        }
        res.add(plain);
      }
      return res;
    } else if (isRepeat()) {
      return detailForRepeat();
    } else {
      return List.of();
    }
  }



  private Set<String> findPosible(String name) {
    return traverser.findState(name).findPosibleTransitions();
  }

  public String nextId(String prefix, String exists) {
    return traverser.nextId(prefix, exists);
  }

  @Override
  public String toString() {
    return "UmlPath [" + id + "]";
  }

  @Data
  static class Traverser {
    private Set<UmlPath> cicled = new HashSet<>();
    private Map<String, Activity> allStates;
    private final IdGen idGen = new IdGen();

    public String nextId(String prefix, String exists) {
      return idGen.nextId(prefix, exists);
    }

    public Traverser(WorkflowExpression def) {
      allStates = def.getActivities().stream().collect(Collectors.toMap((el -> el.getId()), el -> el));
    }

    private Activity findState(String id) {
      return allStates.get(id);
    }
  }
}
