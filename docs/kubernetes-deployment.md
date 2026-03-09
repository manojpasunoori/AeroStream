# Kubernetes + GitOps Deployment

## Helm Chart

- Chart path: `infra/helm/aerostream`
- Environment values:
  - `values-dev.yaml`
  - `values-staging.yaml`
  - `values-prod.yaml`

## Overlays

Kustomize overlays are available under `infra/k8s/overlays`:

- `dev` (AKS free-tier profile)
- `staging` (EKS free-tier profile)
- `prod`

## ArgoCD

Application manifest:

- `infra/argocd/aerostream-app.yaml`

## Example Commands

- Render helm chart:
  - `helm template aerostream infra/helm/aerostream -f infra/helm/aerostream/values-dev.yaml`
- Apply dev overlay:
  - `kubectl apply -k infra/k8s/overlays/dev`

## Free Tier Testing Notes

- Azure Free Tier: use AKS with low node size and reduced replica counts.
- AWS Free Tier: use EKS with minimal worker footprint and disable nonessential services.
- For both tiers, disable simulator in idle windows to reduce costs.
